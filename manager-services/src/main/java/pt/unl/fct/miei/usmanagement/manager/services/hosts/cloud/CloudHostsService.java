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

package pt.unl.fct.miei.usmanagement.manager.services.hosts.cloud;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.InstanceType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.unl.fct.miei.usmanagement.manager.config.ParallelismProperties;
import pt.unl.fct.miei.usmanagement.manager.eips.ElasticIp;
import pt.unl.fct.miei.usmanagement.manager.exceptions.EntityNotFoundException;
import pt.unl.fct.miei.usmanagement.manager.exceptions.ManagerException;
import pt.unl.fct.miei.usmanagement.manager.hosts.Coordinates;
import pt.unl.fct.miei.usmanagement.manager.hosts.HostAddress;
import pt.unl.fct.miei.usmanagement.manager.hosts.cloud.AwsRegion;
import pt.unl.fct.miei.usmanagement.manager.hosts.cloud.CloudHost;
import pt.unl.fct.miei.usmanagement.manager.hosts.cloud.CloudHosts;
import pt.unl.fct.miei.usmanagement.manager.metrics.simulated.HostSimulatedMetric;
import pt.unl.fct.miei.usmanagement.manager.nodes.NodeRole;
import pt.unl.fct.miei.usmanagement.manager.regions.RegionEnum;
import pt.unl.fct.miei.usmanagement.manager.rulesystem.rules.HostRule;
import pt.unl.fct.miei.usmanagement.manager.services.communication.kafka.KafkaService;
import pt.unl.fct.miei.usmanagement.manager.services.configurations.ConfigurationsService;
import pt.unl.fct.miei.usmanagement.manager.services.containers.ContainersService;
import pt.unl.fct.miei.usmanagement.manager.services.docker.nodes.NodesService;
import pt.unl.fct.miei.usmanagement.manager.services.docker.swarm.DockerSwarmService;
import pt.unl.fct.miei.usmanagement.manager.services.eips.ElasticIpsService;
import pt.unl.fct.miei.usmanagement.manager.services.hosts.HostsService;
import pt.unl.fct.miei.usmanagement.manager.services.hosts.cloud.aws.AwsInstanceState;
import pt.unl.fct.miei.usmanagement.manager.services.hosts.cloud.aws.AwsService;
import pt.unl.fct.miei.usmanagement.manager.services.hosts.cloud.aws.AwsSimpleInstance;
import pt.unl.fct.miei.usmanagement.manager.services.monitoring.metrics.simulated.HostSimulatedMetricsService;
import pt.unl.fct.miei.usmanagement.manager.services.rulesystem.rules.HostRulesService;
import pt.unl.fct.miei.usmanagement.manager.util.EntityUtils;
import pt.unl.fct.miei.usmanagement.manager.workermanagers.WorkerManager;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CloudHostsService {

	private final AwsService awsService;
	private final HostRulesService hostRulesService;
	private final HostSimulatedMetricsService hostSimulatedMetricsService;
	private final HostsService hostsService;
	private final NodesService nodesService;
	private final DockerSwarmService dockerSwarmService;
	private final ConfigurationsService configurationsService;
	private final ContainersService containersService;
	private final ElasticIpsService elasticIpsService;
	private final KafkaService kafkaService;

	private final CloudHosts cloudHosts;

	private final int threads;

	public CloudHostsService(@Lazy AwsService awsService,
							 @Lazy HostRulesService hostRulesService,
							 @Lazy HostSimulatedMetricsService hostSimulatedMetricsService,
							 @Lazy HostsService hostsService,
							 @Lazy NodesService nodesService,
							 @Lazy DockerSwarmService dockerSwarmService,
							 ConfigurationsService configurationsService,
							 @Lazy ContainersService containersService,
							 ElasticIpsService elasticIpsService, KafkaService kafkaService, CloudHosts cloudHosts,
							 ParallelismProperties parallelismProperties) {
		this.awsService = awsService;
		this.hostRulesService = hostRulesService;
		this.hostSimulatedMetricsService = hostSimulatedMetricsService;
		this.hostsService = hostsService;
		this.nodesService = nodesService;
		this.dockerSwarmService = dockerSwarmService;
		this.configurationsService = configurationsService;
		this.containersService = containersService;
		this.elasticIpsService = elasticIpsService;
		this.kafkaService = kafkaService;
		this.cloudHosts = cloudHosts;
		this.threads = parallelismProperties.getThreads();
	}

	@Transactional(readOnly = true)
	public List<CloudHost> getCloudHosts() {
		return cloudHosts.findAll();
	}

	public List<CloudHost> getCloudHostsAndRelations() {
		return cloudHosts.getCloudHostsAndRelations();
	}

	public List<CloudHost> getInactiveCloudHosts() {
		return getCloudHosts().stream()
			.filter(host -> !nodesService.isPartOfSwarm(host.getAddress()))
			.collect(Collectors.toList());
	}

	public CloudHost getCloudHostById(String id) {
		return cloudHosts.findByInstanceId(id).orElseThrow(() ->
			new EntityNotFoundException(CloudHost.class, "id", id));
	}

	public CloudHost getCloudHostByIdOrDns(String value) {
		return cloudHosts.findByInstanceIdOrPublicDnsName(value, value).orElseThrow(() ->
			new EntityNotFoundException(CloudHost.class, "value", value));
	}

	public CloudHost getCloudHostByIp(String ipAddress) {
		return cloudHosts.findByPublicIpAddress(ipAddress).orElseThrow(() ->
			new EntityNotFoundException(CloudHost.class, "ipAddress", ipAddress));
	}

	public CloudHost getCloudHostByIdOrIp(String value) {
		return cloudHosts.findByInstanceIdOrPublicIpAddress(value, value).orElseThrow(() ->
			new EntityNotFoundException(CloudHost.class, "value", value));
	}

	public CloudHost getCloudHostByAddress(HostAddress address) {
		return cloudHosts.findByAddress(address.getPublicIpAddress(), address.getPrivateIpAddress()).orElseThrow(() ->
			new EntityNotFoundException(CloudHost.class, "address", address.toString()));
	}

	public List<CloudHost> getCloudHostByTypeAndRegion(InstanceType type, RegionEnum region) {
		return this.cloudHosts.findByInstanceType(type.toString().toLowerCase()).stream()
			.filter(cloudHost -> cloudHost.getAwsRegion().getRegion() == region)
			.collect(Collectors.toList());
	}

	public CloudHost saveCloudHost(CloudHost cloudHost) {
		log.info("Saving cloudHost {}", ToStringBuilder.reflectionToString(cloudHost));
		return cloudHosts.save(cloudHost);
	}

	public CloudHost addIfNotPresent(CloudHost cloudHost) {
		Optional<CloudHost> cloudHostOptional = cloudHosts.findById(cloudHost.getId());
		return cloudHostOptional.orElseGet(() -> {
			cloudHost.clearAssociations();
			return saveCloudHost(cloudHost);
		});
	}

	public CloudHost addOrUpdateCloudHost(CloudHost cloudHost) {
		if (cloudHost.getId() != null) {
			Optional<CloudHost> cloudHostOptional = cloudHosts.findById(cloudHost.getId());
			if (cloudHostOptional.isPresent()) {
				CloudHost existingCloudHost = cloudHostOptional.get();
				Set<HostRule> rules = cloudHost.getHostRules();
				if (rules != null) {
					Set<HostRule> currentRules = existingCloudHost.getHostRules();
					if (currentRules == null) {
						existingCloudHost.setHostRules(new HashSet<>(rules));
					}
					else {
						rules.iterator().forEachRemaining(rule -> {
							if (!currentRules.contains(rule)) {
								rule.addCloudHost(existingCloudHost);
							}
						});
						currentRules.iterator().forEachRemaining(currentRule -> {
							if (!rules.contains(currentRule)) {
								currentRule.removeCloudHost(existingCloudHost);
							}
						});
					}
				}
				Set<HostSimulatedMetric> simulatedMetrics = cloudHost.getSimulatedHostMetrics();
				if (simulatedMetrics != null) {
					Set<HostSimulatedMetric> currentSimulatedMetrics = existingCloudHost.getSimulatedHostMetrics();
					if (currentSimulatedMetrics == null) {
						existingCloudHost.setSimulatedHostMetrics(new HashSet<>(simulatedMetrics));
					}
					else {
						simulatedMetrics.iterator().forEachRemaining(simulatedMetric -> {
							if (!currentSimulatedMetrics.contains(simulatedMetric)) {
								simulatedMetric.addCloudHost(existingCloudHost);
							}
						});
						currentSimulatedMetrics.iterator().forEachRemaining(currentSimulatedMetric -> {
							if (!simulatedMetrics.contains(currentSimulatedMetric)) {
								currentSimulatedMetric.removeCloudHost(existingCloudHost);
							}
						});
					}
				}
				EntityUtils.copyValidProperties(cloudHost, existingCloudHost);
				return saveCloudHost(existingCloudHost);
			}
		}
		return saveCloudHost(cloudHost);
	}

	public CloudHost updateCloudHost(CloudHost cloudHost) {
		checkCloudHostExists(cloudHost.getInstanceId());
		cloudHost = saveCloudHost(cloudHost);
		kafkaService.sendCloudHost(cloudHost);
		return cloudHost;
	}

	private CloudHost saveCloudHostFromInstance(Instance instance) {
		return saveCloudHostFromInstance(null, instance);
	}

	public CloudHost saveCloudHostFromInstance(Long id, Instance instance) {
		CloudHost cloudHost = CloudHost.builder()
			.id(id)
			.instanceId(instance.getInstanceId())
			.instanceType(instance.getInstanceType())
			.state(instance.getState())
			.imageId(instance.getImageId())
			.publicDnsName(instance.getPublicDnsName())
			.publicIpAddress(instance.getPublicIpAddress())
			.privateIpAddress(instance.getPrivateIpAddress())
			.awsRegion(AwsRegion.fromPlacement(instance.getPlacement()))
			.placement(instance.getPlacement())
			.build();
		/*cloudHost.setNew(id == null);*/
		Optional<CloudHost> savedCloudHost = cloudHosts.findByInstanceId(cloudHost.getInstanceId());
		if (savedCloudHost.isPresent()) {
			cloudHost.setId(savedCloudHost.get().getId());
		}
		cloudHost = addOrUpdateCloudHost(cloudHost);
		/*CloudHost kafkaCloudHost = cloudHost;
		kafkaCloudHost.setNew(id == null);
		kafkaService.sendCloudHost(kafkaCloudHost);*/
		kafkaService.sendCloudHost(cloudHost);
		return cloudHost;
	}

	public CloudHost addCloudHostFromSimpleInstance(AwsSimpleInstance simpleInstance) {
		CloudHost cloudHost = CloudHost.builder()
			.instanceId(simpleInstance.getInstanceId())
			.instanceType(simpleInstance.getInstanceType())
			.state(simpleInstance.getState())
			.imageId(simpleInstance.getImageId())
			.publicDnsName(simpleInstance.getPublicDnsName())
			.publicIpAddress(simpleInstance.getPublicIpAddress())
			.privateIpAddress(simpleInstance.getPrivateIpAddress())
			.awsRegion(AwsRegion.fromPlacement(simpleInstance.getPlacement()))
			.placement(simpleInstance.getPlacement())
			.build();
		cloudHost = saveCloudHost(cloudHost);
		/*CloudHost kafkaCloudHost = cloudHost;
		kafkaCloudHost.setNew(true);
		kafkaService.sendCloudHost(kafkaCloudHost);*/
		kafkaService.sendCloudHost(cloudHost);
		return cloudHost;
	}

	public void deleteCloudHost(Long id) {
		cloudHosts.deleteById(id);
	}

	public void deleteCloudHost(CloudHost cloudHost) {
		deleteCloudHost(cloudHost.getId());
		kafkaService.sendDeleteCloudHost(cloudHost);
		try {
			RegionEnum region = cloudHost.getAwsRegion().getRegion();
			ElasticIp elasticIp = elasticIpsService.getElasticIp(region);
			String instanceId = cloudHost.getInstanceId();
			if (Objects.equals(elasticIp.getInstanceId(), instanceId)) {
				elasticIpsService.dissociate(cloudHost);
			}
		}
		catch (EntityNotFoundException ignored) {
		}
	}

	public AwsRegion getClosestAwsRegion(Coordinates coordinates) {
		List<AwsRegion> awsRegions = AwsRegion.getAwsRegions();
		awsRegions.sort((oneRegion, anotherRegion) -> {
			double oneDistance = oneRegion.getCoordinates().distanceTo(coordinates);
			double anotherDistance = anotherRegion.getCoordinates().distanceTo(coordinates);
			return Double.compare(oneDistance, anotherDistance);
		});
		log.info("Aws regions sorted by distance to {}: {}", coordinates, awsRegions.stream().map(AwsRegion::getName).collect(Collectors.toList()));
		return awsRegions.get(0);
	}

	public CloudHost launchInstance(Coordinates coordinates) {
		log.info("Looking for the best aws region to start a cloud instance close to {}", coordinates);
		AwsRegion awsRegion = getClosestAwsRegion(coordinates);
		log.info("{} {} is the closest aws region with a distance of {} km", awsRegion.getZone(), awsRegion.getName(),
			(int) awsRegion.getCoordinates().distanceTo(coordinates) / 1000);
		return launchInstance(awsRegion);
	}

	public CloudHost launchInstance(AwsRegion region) {
		return launchInstance(region, InstanceType.T2Micro);
	}

	public CloudHost launchInstance(AwsRegion region, InstanceType type) {
		return launchInstance(region, type, true);
	}

	public CloudHost launchInstance(AwsRegion region, InstanceType type, boolean joinSwarm) {
		String instanceId = null;
		try {
			Instance instance = awsService.createInstance(region, type);
			instanceId = instance.getInstanceId();
			CloudHost cloudHost = saveCloudHostFromInstance(instance);
			if (joinSwarm) {
				hostsService.addHost(instance.getPublicIpAddress(), NodeRole.WORKER);
			}
			return cloudHost;
		}
		finally {
			if (instanceId != null) {
				configurationsService.removeConfiguration(instanceId);
			}
		}
	}

	public CloudHost startInstance(String id, boolean addToSwarm) {
		CloudHost cloudHost = getCloudHostById(id);
		return startInstance(cloudHost, addToSwarm);
	}

	public CloudHost startInstance(CloudHost cloudHost, boolean addToSwarm) {
		InstanceState state = new InstanceState()
			.withCode(AwsInstanceState.PENDING.getCode())
			.withName(AwsInstanceState.PENDING.getState());
		cloudHost.setState(state);
		cloudHost = cloudHosts.save(cloudHost);
		Instance instance = awsService.startInstance(cloudHost.getInstanceId(), cloudHost.getAwsRegion(), true);
		cloudHost = saveCloudHostFromInstance(cloudHost.getId(), instance);
		if (addToSwarm) {
			hostsService.addHost(cloudHost.getInstanceId(), NodeRole.WORKER);
		}
		return cloudHost;
	}

	public CloudHost stopInstance(String id) {
		CloudHost cloudHost = getCloudHostById(id);
		try {
			hostsService.removeHost(cloudHost.getAddress());
		}
		catch (ManagerException e) {
			log.error("Failed to remove host {}: {}", id, e.getMessage());
		}
		InstanceState state = new InstanceState()
			.withCode(AwsInstanceState.STOPPING.getCode())
			.withName(AwsInstanceState.STOPPING.getState());
		cloudHost.setState(state);
		cloudHost = cloudHosts.save(cloudHost);
		Instance instance = awsService.stopInstance(cloudHost.getInstanceId(), cloudHost.getAwsRegion(), true);
		return saveCloudHostFromInstance(cloudHost.getId(), instance);
	}

	public void terminateInstance(String id, boolean wait) {
		CloudHost cloudHost = getCloudHostById(id);
		try {
			hostsService.removeHost(cloudHost.getAddress());
		}
		catch (ManagerException e) {
			log.error("Failed to remove instance {} from the system: {}", id, e.getMessage());
		}
		InstanceState state = new InstanceState().withCode(AwsInstanceState.SHUTTING_DOWN.getCode())
			.withName(AwsInstanceState.SHUTTING_DOWN.getState());
		cloudHost.setState(state);
		cloudHost = cloudHosts.save(cloudHost);
		awsService.terminateInstance(cloudHost.getInstanceId(), cloudHost.getAwsRegion(), wait);
		HostAddress address = cloudHost.getAddress();
		if (nodesService.isPartOfSwarm(address)) {
			dockerSwarmService.leaveSwarm(address);
		}
		deleteCloudHost(cloudHost);
	}

	public void terminateInstances() {
		log.info("Terminating cloud instances");
		new ForkJoinPool(threads).submit(() ->
			awsService.getInstances().parallelStream()
				.filter(instance -> !Objects.equals(instance.getState().getCode(), AwsInstanceState.TERMINATED.getCode()))
				.forEach(instance -> {
					String instanceId = instance.getInstanceId();
					try {
						terminateInstance(instanceId, false);
					}
					catch (EntityNotFoundException ignored) {
						AwsRegion region = AwsRegion.fromPlacement(instance.getPlacement());
						awsService.terminateInstance(instanceId, region, false);
					}
				})).join();
	}

	public List<HostRule> getRules(String hostname) {
		checkCloudHostExists(hostname);
		return cloudHosts.getRules(hostname);
	}

	public HostRule getRule(String hostname, String ruleName) {
		checkCloudHostExists(hostname);
		return cloudHosts.getRule(hostname, ruleName).orElseThrow(() ->
			new EntityNotFoundException(HostRule.class, "ruleName", ruleName)
		);
	}

	public void addRule(String hostname, String ruleName) {
		checkCloudHostExists(hostname);
		hostRulesService.addCloudHost(ruleName, hostname);
	}

	public void addRules(String hostname, List<String> ruleNames) {
		checkCloudHostExists(hostname);
		ruleNames.forEach(rule -> hostRulesService.addCloudHost(rule, hostname));
	}

	public void removeRule(String hostname, String ruleName) {
		removeRules(hostname, List.of(ruleName));
	}

	public void removeRules(String hostname, List<String> ruleNames) {
		checkCloudHostExists(hostname);
		ruleNames.forEach(rule -> hostRulesService.removeCloudHost(rule, hostname));
	}

	public List<HostSimulatedMetric> getSimulatedMetrics(String hostname) {
		checkCloudHostExists(hostname);
		return cloudHosts.getSimulatedMetrics(hostname);
	}

	public HostSimulatedMetric getSimulatedMetric(String hostname, String simulatedMetricName) {
		checkCloudHostExists(hostname);
		return cloudHosts.getSimulatedMetric(hostname, simulatedMetricName).orElseThrow(() ->
			new EntityNotFoundException(HostSimulatedMetric.class, "simulatedMetricName", simulatedMetricName)
		);
	}

	public void addSimulatedMetric(String hostname, String simulatedMetricName) {
		addSimulatedMetrics(hostname, List.of(simulatedMetricName));
	}

	public void addSimulatedMetrics(String hostname, List<String> simulatedMetricNames) {
		checkCloudHostExists(hostname);
		simulatedMetricNames.forEach(simulatedMetric ->
			hostSimulatedMetricsService.addCloudHost(simulatedMetric, hostname));
	}

	public void removeSimulatedMetric(String hostname, String simulatedMetricName) {
		removeSimulatedMetrics(hostname, List.of(simulatedMetricName));
	}

	public void removeSimulatedMetrics(String hostname, List<String> simulatedMetricNames) {
		checkCloudHostExists(hostname);
		simulatedMetricNames.forEach(simulatedMetric -> hostSimulatedMetricsService.removeCloudHost(simulatedMetric, hostname));
	}

	public void assignWorkerManager(WorkerManager workerManager, String hostname) {
		log.info("Assigning worker manager {} to cloud host {}", workerManager.getId(), hostname);
		CloudHost cloudHost = getCloudHostByIp(hostname).toBuilder()
			.managedByWorker(workerManager)
			.build();
		cloudHosts.save(cloudHost);
	}

	public void unassignWorkerManager(String hostname) {
		CloudHost cloudHost = getCloudHostByIp(hostname).toBuilder()
			.managedByWorker(null)
			.build();
		cloudHosts.save(cloudHost);
	}

	public CloudHost updateAddress(CloudHost cloudHost, String publicIpAddress) {
		cloudHost.setPublicIpAddress(publicIpAddress);
		return updateCloudHost(cloudHost);
	}

	public boolean hasCloudHost(String hostname) {
		return cloudHosts.hasCloudHost(hostname);
	}

	private void checkCloudHostExists(String hostname) {
		if (!hasCloudHost(hostname)) {
			throw new EntityNotFoundException(CloudHost.class, "hostname", hostname);
		}
	}
}
