/*
 * MIT License
 *
 * Copyright (c) 2020 manager
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package pt.unl.fct.miei.usmanagement.manager.management.docker.containers;

import com.google.gson.Gson;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.AttachedNetwork;
import com.spotify.docker.client.messages.Container;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.ContainerInfo;
import com.spotify.docker.client.messages.ContainerStats;
import com.spotify.docker.client.messages.HostConfig;
import com.spotify.docker.client.messages.PortBinding;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import pt.unl.fct.miei.usmanagement.manager.containers.ContainerConstants;
import pt.unl.fct.miei.usmanagement.manager.containers.ContainerEntity;
import pt.unl.fct.miei.usmanagement.manager.containers.ContainerPortMapping;
import pt.unl.fct.miei.usmanagement.manager.containers.ContainerType;
import pt.unl.fct.miei.usmanagement.manager.exceptions.ManagerException;
import pt.unl.fct.miei.usmanagement.manager.hosts.Coordinates;
import pt.unl.fct.miei.usmanagement.manager.hosts.HostAddress;
import pt.unl.fct.miei.usmanagement.manager.management.containers.ContainerProperties;
import pt.unl.fct.miei.usmanagement.manager.management.containers.ContainersService;
import pt.unl.fct.miei.usmanagement.manager.management.docker.DockerCoreService;
import pt.unl.fct.miei.usmanagement.manager.management.docker.swarm.DockerSwarmService;
import pt.unl.fct.miei.usmanagement.manager.management.docker.swarm.nodes.NodesService;
import pt.unl.fct.miei.usmanagement.manager.management.hosts.HostsService;
import pt.unl.fct.miei.usmanagement.manager.management.loadbalancer.nginx.NginxLoadBalancerService;
import pt.unl.fct.miei.usmanagement.manager.management.services.ServicesService;
import pt.unl.fct.miei.usmanagement.manager.management.services.discovery.registration.RegistrationServerService;
import pt.unl.fct.miei.usmanagement.manager.regions.Region;
import pt.unl.fct.miei.usmanagement.manager.services.ServiceEntity;
import pt.unl.fct.miei.usmanagement.manager.services.ServiceType;
import pt.unl.fct.miei.usmanagement.manager.util.Timing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Slf4j
public class DockerContainersService {

	private static final long DELAY_BETWEEN_CONTAINER_LAUNCH = TimeUnit.SECONDS.toMillis(5);

	private final ContainersService containersService;
	private final DockerCoreService dockerCoreService;
	private final NodesService nodesService;
	private final ServicesService servicesService;
	private final NginxLoadBalancerService nginxLoadBalancerService;
	private final RegistrationServerService registrationServerService;
	private final HostsService hostsService;

	private final int dockerDelayBeforeStopContainer;

	@Getter
	private boolean launchingContainer;

	public DockerContainersService(@Lazy ContainersService containersService, DockerCoreService dockerCoreService,
								   NodesService nodesService, ServicesService servicesService,
								   NginxLoadBalancerService nginxLoadBalancerService,
								   RegistrationServerService registrationServerService, HostsService hostsService,
								   ContainerProperties containerProperties) {
		this.containersService = containersService;
		this.dockerCoreService = dockerCoreService;
		this.nodesService = nodesService;
		this.servicesService = servicesService;
		this.nginxLoadBalancerService = nginxLoadBalancerService;
		this.registrationServerService = registrationServerService;
		this.hostsService = hostsService;
		this.dockerDelayBeforeStopContainer = containerProperties.getDelayBeforeStop();
		this.launchingContainer = false;
	}

	public Map<String, List<DockerContainer>> launchApp(List<ServiceEntity> services, Coordinates coordinates) {
		Map<String, List<DockerContainer>> serviceContainers = new HashMap<>();
		Map<String, String> dynamicLaunchParams = new HashMap<>();
		services.forEach(service -> {
			List<DockerContainer> containers = launchMicroservice(service, coordinates, dynamicLaunchParams);
			serviceContainers.put(service.getServiceName(), containers);
			containers.forEach(container -> {
				String hostname = container.getPublicIpAddress();
				int privatePort = container.getPorts().get(0).getPrivatePort(); //TODO privado ou publico?
				String address = String.format("%s:%d", hostname, privatePort);
				dynamicLaunchParams.put(service.getOutputLabel(), address);
			});
			//TODO rever tempo de espera, é preciso? Timing.sleep(DELAY_BETWEEN_CONTAINER_LAUNCH, TimeUnit.MILLISECONDS);
		});
		return serviceContainers;
	}

	private List<DockerContainer> launchMicroservice(ServiceEntity service, Coordinates coordinates,
													 Map<String, String> dynamicLaunchParams) {
		List<String> environment = Collections.emptyList();
		Map<String, String> labels = Collections.emptyMap();
		double expectedMemoryConsumption = service.getExpectedMemoryConsumption();
		int minimumReplicas = servicesService.getMinimumReplicasByServiceName(service.getServiceName());
		List<DockerContainer> containers = new ArrayList<>(minimumReplicas);
		for (int i = 0; i < minimumReplicas; i++) {
			HostAddress address = hostsService.getClosestCapableHost(expectedMemoryConsumption, coordinates);
			Optional<DockerContainer> container = launchContainer(address, service, ContainerType.BY_REQUEST, environment,
				labels, dynamicLaunchParams);
			container.ifPresent(containers::add);
		}
		return containers;
	}

	public Optional<DockerContainer> launchContainer(Coordinates coordinates, String serviceName, int externalPort, int internalPort) {
		double expectedMemoryUsage = servicesService.getService(serviceName).getExpectedMemoryConsumption();
		HostAddress hostAddress = hostsService.getClosestCapableHost(expectedMemoryUsage, coordinates);
		return launchContainer(hostAddress, serviceName, externalPort, internalPort);
	}

	public Optional<DockerContainer> launchContainer(HostAddress address, String serviceName) {
		return launchContainer(address, serviceName, ContainerType.BY_REQUEST);
	}

	public Optional<DockerContainer> launchContainer(HostAddress address, String serviceName, ContainerType containerType) {
		List<String> environment = Collections.emptyList();
		return launchContainer(address, serviceName, containerType, environment);
	}

	public Optional<DockerContainer> launchContainer(HostAddress address, String serviceName, List<String> environment) {
		return launchContainer(address, serviceName, ContainerType.BY_REQUEST, environment);
	}

	public Optional<DockerContainer> launchContainer(HostAddress address, String serviceName,
													 ContainerType containerType, List<String> environment) {
		Map<String, String> labels = Collections.emptyMap();
		return launchContainer(address, serviceName, containerType, environment, labels);
	}

	public Optional<DockerContainer> launchContainer(HostAddress address, String serviceName, Map<String, String> labels) {
		return launchContainer(address, serviceName, ContainerType.BY_REQUEST, labels);
	}

	public Optional<DockerContainer> launchContainer(HostAddress address, String serviceName,
													 ContainerType containerType, Map<String, String> labels) {
		List<String> environment = Collections.emptyList();
		return launchContainer(address, serviceName, containerType, environment, labels);
	}

	public Optional<DockerContainer> launchContainer(HostAddress address, String serviceName, List<String> environment,
													 Map<String, String> labels) {
		return launchContainer(address, serviceName, ContainerType.BY_REQUEST, environment, labels);
	}

	public Optional<DockerContainer> launchContainer(HostAddress address, String serviceName, List<String> environment,
													 Map<String, String> labels,
													 Map<String, String> dynamicLaunchParams) {
		return launchContainer(address, serviceName, ContainerType.BY_REQUEST, environment, labels, dynamicLaunchParams);
	}

	public Optional<DockerContainer> launchContainer(HostAddress address, String serviceName,
													 ContainerType containerType, List<String> environment,
													 Map<String, String> labels) {
		Map<String, String> dynamicLaunchParams = Collections.emptyMap();
		return launchContainer(address, serviceName, containerType, environment, labels, dynamicLaunchParams);
	}

	public Optional<DockerContainer> launchContainer(HostAddress address, String serviceName, ContainerType containerType,
													 List<String> environment, Map<String, String> labels,
													 Map<String, String> dynamicLaunchParams) {
		ServiceEntity service = servicesService.getService(serviceName);
		return launchContainer(address, service, containerType, environment, labels, dynamicLaunchParams);
	}

	public Optional<DockerContainer> launchContainer(HostAddress address, String serviceName, int externalPort, int internalPort) {
		return launchContainer(address, serviceName, ContainerType.BY_REQUEST, externalPort, internalPort);
	}

	public Optional<DockerContainer> launchContainer(HostAddress address, String serviceName, int internalPort,
													 int externalPort, List<String> environment) {
		return launchContainer(address, serviceName, ContainerType.BY_REQUEST, externalPort, internalPort, environment);
	}

	public Optional<DockerContainer> launchContainer(HostAddress address, String serviceName, ContainerType containerType,
													 int externalPort, int internalPort) {
		return launchContainer(address, serviceName, containerType, externalPort, internalPort, Collections.emptyList());
	}

	public Optional<DockerContainer> launchContainer(HostAddress address, String serviceName, ContainerType containerType,
													 int externalPort, int internalPort, List<String> environment) {
		ServiceEntity service = servicesService.getService(serviceName).toBuilder()
			.defaultExternalPort(externalPort)
			.defaultInternalPort(internalPort)
			.build();
		Map<String, String> labels = Collections.emptyMap();
		Map<String, String> dynamicLaunchParams = Collections.emptyMap();
		return launchContainer(address, service, containerType, environment, labels, dynamicLaunchParams);
	}

	private Optional<DockerContainer> launchContainer(HostAddress hostAddress, ServiceEntity service,
													  ContainerType containerType, List<String> environment,
													  Map<String, String> labels,
													  Map<String, String> dynamicLaunchParams) {
		launchingContainer = true;
		int retries = 5;
		String errorMessage = null;
		try {
			for (var i = 0; i < retries; i++) {

				if (!hostAddress.isComplete()) {
					hostAddress = hostsService.getFullHostAddress(hostAddress);
				}
				String serviceName = service.getServiceName();
				log.info("Launching container on mode {} with service {} at {}", containerType, serviceName, hostAddress);

				if (containerType == ContainerType.SINGLETON) {
					List<DockerContainer> containers = List.of();
					try {
						containers = getContainers(
							DockerClient.ListContainersParam.withLabel(ContainerConstants.Label.SERVICE_NAME, serviceName),
							DockerClient.ListContainersParam.withLabel(ContainerConstants.Label.SERVICE_PUBLIC_IP_ADDRESS, hostAddress.getPublicIpAddress()),
							DockerClient.ListContainersParam.withLabel(ContainerConstants.Label.SERVICE_PRIVATE_IP_ADDRESS, hostAddress.getPrivateIpAddress()));
					}
					catch (ManagerException e) {
						log.error(e.getMessage());
					}
					if (containers.size() > 0) {
						DockerContainer container = containers.get(0);
						log.info("Service {} is already running on container {} on host {}", serviceName, container.getId(), hostAddress);
						return Optional.of(container);
					}
				}

				String serviceType = service.getServiceType().name();
				int externalPort = hostsService.findAvailableExternalPort(hostAddress, service.getDefaultExternalPort());
				int internalPort = service.getDefaultInternalPort();
				String containerName = containerType == ContainerType.SINGLETON
					? serviceName
					: String.format("%s_%s_%s_%s", serviceName, hostAddress.getPublicIpAddress(), hostAddress.getPrivateIpAddress(), externalPort);
				String serviceAddress = String.format("%s:%s", hostAddress.getPublicIpAddress(), externalPort);
				String dockerRepository = service.getDockerRepository();
				String launchCommand = service.getLaunchCommand();
				if (launchCommand == null) {
					launchCommand = "";
				}
				launchCommand = launchCommand
					.replace("${hostname}", hostAddress.getPublicIpAddress())
					.replace("${externalPort}", String.valueOf(externalPort))
					.replace("${internalPort}", String.valueOf(internalPort));
				log.info("Launch command: {}", launchCommand);

				Region region = hostAddress.getRegion();
				if (servicesService.serviceDependsOn(serviceName, RegistrationServerService.REGISTRATION_SERVER)) {
					String outputLabel = servicesService.getService(RegistrationServerService.REGISTRATION_SERVER).getOutputLabel();
					String registrationAddress = registrationServerService
						.getRegistrationServerAddress(region)
						.orElseGet(() -> registrationServerService.launchRegistrationServer(region).getLabels().get(ContainerConstants.Label.SERVICE_ADDRESS));
					launchCommand = launchCommand.replace(outputLabel, registrationAddress);
				}

				for (ServiceEntity databaseService : servicesService.getDependenciesByType(serviceName, ServiceType.DATABASE)) {
					String databaseServiceName = databaseService.getServiceName();
					String databaseHost = getDatabaseHostForService(hostAddress, databaseServiceName);
					String outputLabel = databaseService.getOutputLabel();
					launchCommand = launchCommand.replace(outputLabel, databaseHost);
				}
				for (Map.Entry<String, String> param : dynamicLaunchParams.entrySet()) {
					launchCommand = launchCommand.replace(param.getKey(), param.getValue());
				}

				List<String> containerEnvironment = new LinkedList<>();
				containerEnvironment.add(ContainerConstants.Environment.SERVICE_REGION + "=" + region);
				containerEnvironment.addAll(environment);

				Map<String, String> containerLabels = new HashMap<>();
				containerLabels.put(ContainerConstants.Label.US_MANAGER, String.valueOf(true));
				containerLabels.put(ContainerConstants.Label.CONTAINER_TYPE, containerType.name());
				containerLabels.put(ContainerConstants.Label.SERVICE_NAME, serviceName);
				containerLabels.put(ContainerConstants.Label.SERVICE_TYPE, serviceType);
				containerLabels.put(ContainerConstants.Label.SERVICE_ADDRESS, serviceAddress);
				containerLabels.put(ContainerConstants.Label.SERVICE_PUBLIC_IP_ADDRESS, hostAddress.getPublicIpAddress());
				containerLabels.put(ContainerConstants.Label.SERVICE_PRIVATE_IP_ADDRESS, hostAddress.getPrivateIpAddress());
				containerLabels.put(ContainerConstants.Label.COORDINATES, new Gson().toJson(hostAddress.getCoordinates()));
				containerLabels.put(ContainerConstants.Label.REGION, region.name());
				containerLabels.putAll(labels);

				log.info("host = {}, internalPort = {}, externalPort = {}, containerName = {}, "
						+ "dockerRepository = {}, launchCommand = {}, envs = {}, labels = {}",
					hostAddress, internalPort, externalPort, containerName, dockerRepository, launchCommand, containerEnvironment,
					containerLabels);

				HostConfig hostConfig = HostConfig.builder()
					.autoRemove(true)
					.portBindings(Map.of(String.valueOf(internalPort), List.of(PortBinding.of("", String.valueOf(externalPort)))))
					.build();
				ContainerConfig.Builder containerBuilder = ContainerConfig.builder()
					.image(dockerRepository)
					.exposedPorts(String.valueOf(internalPort))
					.hostConfig(hostConfig)
					.hostname(serviceName)
					.env(containerEnvironment)
					.labels(containerLabels);
				ContainerConfig containerConfig = launchCommand.isEmpty()
					? containerBuilder.build()
					: containerBuilder.cmd(launchCommand.split(" ")).build();

				try (DockerClient dockerClient = dockerCoreService.getDockerClient(hostAddress)) {
					dockerClient.pull(dockerRepository);
					ContainerCreation containerCreation = dockerClient.createContainer(containerConfig, containerName);
					String containerId = containerCreation.id();
					dockerClient.connectToNetwork(containerId, DockerSwarmService.NETWORK_OVERLAY);
					dockerClient.startContainer(containerId);
					if (ServiceType.getServiceType(serviceType) == ServiceType.FRONTEND) {
						nginxLoadBalancerService.addServer(serviceName, serviceAddress, hostAddress.getCoordinates(), hostAddress.getRegion());
					}
					return getContainer(containerId);
				}
				catch (DockerException | InterruptedException e) {
					errorMessage = e.getMessage();
					log.error("Failed to start container: {}", errorMessage);
				}
				Timing.sleep(i, TimeUnit.SECONDS);
			}
		}
		finally {
			launchingContainer = false;
		}
		throw new ManagerException("Failed to start container: %s", errorMessage);
	}

	private String getDatabaseHostForService(HostAddress hostAddress, String databaseServiceName) {
		ContainerEntity databaseContainer = containersService.getHostContainersWithLabels(hostAddress,
			Set.of(Pair.of(ContainerConstants.Label.SERVICE_NAME, databaseServiceName)))
			.stream().findFirst().orElseGet(() -> containersService.launchContainer(hostAddress, databaseServiceName));
		if (databaseContainer == null) {
			throw new ManagerException("Failed to launch database {} on host {}", databaseServiceName, hostAddress);
		}
		String address = databaseContainer.getLabels().get(ContainerConstants.Label.SERVICE_ADDRESS);
		log.info("Found database {} on host {}", address, hostAddress);
		return address;
	}

	public void stopContainer(ContainerEntity container) {
		String containerId = container.getContainerId();
		HostAddress hostAddress = container.getHostAddress();
		stopContainer(containerId, hostAddress);
	}

	public void stopContainer(String id, HostAddress hostAddress) {
		this.stopContainer(id, hostAddress, null);
	}

	public void stopContainer(String id, HostAddress hostAddress, Integer delay) {
		ContainerInfo containerInfo = inspectContainer(id, hostAddress);
		String serviceName = containerInfo.config().labels().get(ContainerConstants.Label.SERVICE_NAME);
		ServiceType serviceType = ServiceType.getServiceType(containerInfo.config().labels().get(ContainerConstants.Label.SERVICE_TYPE));
		if (serviceType == ServiceType.FRONTEND) {
			String serviceAddress = containerInfo.config().labels().get(ContainerConstants.Label.SERVICE_ADDRESS);
			Region region = Region.getRegion(containerInfo.config().labels().get(ContainerConstants.Label.REGION));
			nginxLoadBalancerService.removeServer(serviceName, serviceAddress, region);
		}
		try (DockerClient dockerClient = dockerCoreService.getDockerClient(hostAddress)) {
			//TODO espera duas vezes no caso de migração!?!?
			int delayBeforeStop = delay == null ? dockerDelayBeforeStopContainer : delay;
			dockerClient.stopContainer(id, delayBeforeStop);
			log.info("Stopped container {} ({}) on host {}", serviceName, id, hostAddress.toString());
		}
		catch (DockerException | InterruptedException e) {
			e.printStackTrace();
			throw new ManagerException(e.getMessage());
		}
	}

	public Optional<DockerContainer> replicateContainer(ContainerEntity container, HostAddress toHostAddress) {
		return replicateContainer(container.getContainerId(), container.getHostAddress(), toHostAddress);
	}

	public Optional<DockerContainer> replicateContainer(String id, HostAddress fromHostAddress, HostAddress toHostAddress) {
		if (!toHostAddress.isComplete()) {
			toHostAddress = hostsService.getFullHostAddress(toHostAddress);
		}
		ContainerInfo fromContainer = inspectContainer(id, fromHostAddress);
		String serviceName = fromContainer.name().replace("/", "").split("_")[0];
		Map.Entry<String, List<PortBinding>> port = fromContainer.hostConfig().portBindings().entrySet().iterator().next();
		int externalPort = Integer.parseInt(port.getValue().get(0).hostPort());
		int internalPort = Integer.parseInt(port.getKey());
		ServiceEntity service = servicesService.getService(serviceName).toBuilder()
			.defaultInternalPort(internalPort)
			.defaultExternalPort(externalPort)
			.build();
		List<String> customEnvs = Collections.emptyList();
		Map<String, String> customLabels = Collections.emptyMap();
		Map<String, String> dynamicLaunchParams;
		if (!service.hasLaunchCommand()) {
			dynamicLaunchParams = Collections.emptyMap();
		}
		else {
			List<String> args = fromContainer.args();
			List<String> params = Arrays.asList(service.getLaunchCommand().split(" "));
			assert args.size() == params.size();
			// Merge the 2 lists into a map
			dynamicLaunchParams = IntStream
				.range(0, params.size())
				.boxed()
				.collect(Collectors.toMap(params::get, args::get));
		}
		return launchContainer(toHostAddress, service, ContainerType.BY_REQUEST, customEnvs, customLabels, dynamicLaunchParams);
	}

	public Optional<DockerContainer> migrateContainer(ContainerEntity container, HostAddress toHostAddress) {
		if (!toHostAddress.isComplete()) {
			toHostAddress = hostsService.getFullHostAddress(toHostAddress);
		}
		Optional<DockerContainer> replicaContainer = replicateContainer(container, toHostAddress);
		new Timer("StopContainerTimer").schedule(new TimerTask() {
			@Override
			public void run() {
				stopContainer(container);
			}
		}, dockerDelayBeforeStopContainer);
		return replicaContainer;
	}

	public List<DockerContainer> getContainers(DockerClient.ListContainersParam... filter) {
		return getAllContainers(filter);
	}

	public List<DockerContainer> getContainers(HostAddress hostAddress, DockerClient.ListContainersParam... filter) {
		List<DockerClient.ListContainersParam> filtersList = filter == null
			? new ArrayList<>(1)
			: new ArrayList<>(Arrays.asList(filter));
		filtersList.add(DockerClient.ListContainersFilterParam.withLabel(ContainerConstants.Label.US_MANAGER, String.valueOf(true)));
		filter = filtersList.toArray(new DockerClient.ListContainersParam[0]);
		try (DockerClient dockerClient = dockerCoreService.getDockerClient(hostAddress)) {
			return dockerClient.listContainers(filter).stream().map(this::buildDockerContainer).collect(Collectors.toList());
		}
		catch (DockerException | InterruptedException e) {
			e.printStackTrace();
			throw new ManagerException(e.getMessage());
		}
	}

	public Optional<DockerContainer> findContainer(HostAddress hostAddress, DockerClient.ListContainersParam... filter) {
		return getContainers(hostAddress, filter).stream().findFirst();
	}

	private Optional<DockerContainer> findContainer(String id) {
		DockerClient.ListContainersParam idFilter = DockerClient.ListContainersParam.filter("id", id);
		return getContainers(idFilter).stream().findFirst();
	}

	private List<DockerContainer> getAllContainers(DockerClient.ListContainersParam... filter) {
		return nodesService.getReadyNodes().stream()
			.map(node -> getContainers(node.getHostAddress(), filter))
			.flatMap(List::stream)
			.collect(Collectors.toList());
	}

	public Optional<DockerContainer> getContainer(String id) {
		return findContainer(id);
	}

	private ContainerInfo inspectContainer(String containerId, HostAddress hostAddress) {
		try (DockerClient dockerClient = dockerCoreService.getDockerClient(hostAddress)) {
			return dockerClient.inspectContainer(containerId);
		}
		catch (DockerException | InterruptedException e) {
			e.printStackTrace();
			throw new ManagerException(e.getMessage());
		}
	}

	public ContainerStats getContainerStats(ContainerEntity container, HostAddress hostAddress) {
		try (DockerClient dockerClient = dockerCoreService.getDockerClient(hostAddress)) {
			return dockerClient.stats(container.getContainerId());
		}
		catch (DockerException | InterruptedException e) {
			e.printStackTrace();
			throw new ManagerException(e.getMessage());
		}
	}

	private DockerContainer buildDockerContainer(Container container) {
		Gson gson = new Gson();
		String id = container.id();
		long created = container.created();
		List<String> names = container.names().stream()
			.map(name -> name.startsWith("/") ? name.substring(1) : name)
			.collect(Collectors.toList());
		String image = container.image();
		String command = container.command();
		AttachedNetwork attachedNetwork = container.networkSettings().networks().get(DockerSwarmService.NETWORK_OVERLAY);
		String network = attachedNetwork == null
			? null
			: String.format("%s=%s", DockerSwarmService.NETWORK_OVERLAY, attachedNetwork.networkId().substring(0, 10));
		String state = container.state();
		String status = container.status();
		ContainerType type = ContainerType.getContainerType(container.labels().get(ContainerConstants.Label.CONTAINER_TYPE));
		String publicIpAddress = container.labels().get(ContainerConstants.Label.SERVICE_PUBLIC_IP_ADDRESS);
		String privateIpAddress = container.labels().get(ContainerConstants.Label.SERVICE_PRIVATE_IP_ADDRESS);
		Coordinates coordinates = gson.fromJson(container.labels().get(ContainerConstants.Label.COORDINATES), Coordinates.class);
		Region region = Region.getRegion(container.labels().get(ContainerConstants.Label.REGION));
		List<ContainerPortMapping> ports = container.ports().stream()
			.map(p -> new ContainerPortMapping(p.privatePort(), p.publicPort(), p.type(), p.ip()))
			.collect(Collectors.toList());
		Map<String, String> labels = container.labels();
		return new DockerContainer(id, type, created, names, image, command, network, state, status, publicIpAddress, privateIpAddress,
			coordinates, region, ports, labels);
	}

	public String getContainerLogs(ContainerEntity container) {
		HostAddress hostAddress = container.getHostAddress();
		String containerId = container.getContainerId();
		String logs = null;
		try (DockerClient docker = dockerCoreService.getDockerClient(hostAddress);
			 LogStream stream = docker.logs(containerId, DockerClient.LogsParam.stdout(), DockerClient.LogsParam.stderr())) {
			logs = stream.readFully();
			// remove ANSI escape codes
			logs = logs.replaceAll("\u001B\\[[;\\d]*[ -/]*[@-~]", "");
		}
		catch (DockerException | InterruptedException e) {
			log.error("Failted to get logs of container {}: {}", containerId, e.getMessage());
		}
		return logs;
	}

	public List<DockerContainer> stopAll(Predicate<DockerContainer> containersPredicate) {
		List<DockerContainer> containers = getContainers();
		containers.removeIf(Predicate.not(containersPredicate));
		containers.parallelStream().forEach(container -> stopContainer(container.getId(), container.getHostAddress(), 0));
		return containers;
	}

}
