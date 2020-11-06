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

package pt.unl.fct.miei.usmanagement.manager.management.hosts.cloud;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.Placement;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import pt.unl.fct.miei.usmanagement.manager.exceptions.EntityNotFoundException;
import pt.unl.fct.miei.usmanagement.manager.exceptions.ManagerException;
import pt.unl.fct.miei.usmanagement.manager.hosts.Coordinates;
import pt.unl.fct.miei.usmanagement.manager.hosts.HostAddress;
import pt.unl.fct.miei.usmanagement.manager.hosts.cloud.AwsRegion;
import pt.unl.fct.miei.usmanagement.manager.hosts.cloud.CloudHost;
import pt.unl.fct.miei.usmanagement.manager.hosts.cloud.CloudHosts;
import pt.unl.fct.miei.usmanagement.manager.management.docker.swarm.nodes.NodeRole;
import pt.unl.fct.miei.usmanagement.manager.management.docker.swarm.nodes.NodesService;
import pt.unl.fct.miei.usmanagement.manager.management.hosts.HostsService;
import pt.unl.fct.miei.usmanagement.manager.management.hosts.cloud.aws.AwsInstanceState;
import pt.unl.fct.miei.usmanagement.manager.management.hosts.cloud.aws.AwsService;
import pt.unl.fct.miei.usmanagement.manager.management.hosts.cloud.aws.AwsSimpleInstance;
import pt.unl.fct.miei.usmanagement.manager.management.monitoring.metrics.simulated.HostSimulatedMetricsService;
import pt.unl.fct.miei.usmanagement.manager.management.rulesystem.rules.HostRulesService;
import pt.unl.fct.miei.usmanagement.manager.metrics.simulated.HostSimulatedMetric;
import pt.unl.fct.miei.usmanagement.manager.rulesystem.rules.HostRule;
import pt.unl.fct.miei.usmanagement.manager.workermanagers.WorkerManager;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CloudHostsService {

	private final static int CLOUD_HOSTS_DATABASE_SYNC_INTERVAL = 15000;

	private final AwsService awsService;
	private final HostRulesService hostRulesService;
	private final HostSimulatedMetricsService hostSimulatedMetricsService;
	private final HostsService hostsService;
	private final NodesService nodesService;

	private final CloudHosts cloudHosts;

	private Timer syncDatabaseCloudHostsTimer;
	private boolean launchingInstance;

	public CloudHostsService(@Lazy AwsService awsService,
							 @Lazy HostRulesService hostRulesService,
							 @Lazy HostSimulatedMetricsService hostSimulatedMetricsService,
							 @Lazy HostsService hostsService,
							 @Lazy NodesService nodesService,
							 CloudHosts cloudHosts) {
		this.awsService = awsService;
		this.hostRulesService = hostRulesService;
		this.hostSimulatedMetricsService = hostSimulatedMetricsService;
		this.hostsService = hostsService;
		this.nodesService = nodesService;
		this.cloudHosts = cloudHosts;
		this.launchingInstance = false;
	}

	public List<CloudHost> getCloudHosts() {
		return cloudHosts.findAll();
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

	private CloudHost saveCloudHost(CloudHost cloudHost) {
		log.info("Saving cloudHost {}", ToStringBuilder.reflectionToString(cloudHost));
		return cloudHosts.save(cloudHost);
	}

	private CloudHost saveCloudHostFromInstance(Instance instance) {
		return saveCloudHostFromInstance(0L, instance);
	}

	private CloudHost saveCloudHostFromInstance(Long id, Instance instance) {
		CloudHost cloudHost = CloudHost.builder()
			.id(id)
			.instanceId(instance.getInstanceId())
			.instanceType(instance.getInstanceType())
			.state(instance.getState())
			.imageId(instance.getImageId())
			.publicDnsName(instance.getPublicDnsName())
			.publicIpAddress(instance.getPublicIpAddress())
			.privateIpAddress(instance.getPrivateIpAddress())
			.awsRegion(this.getPlacementRegion(instance.getPlacement()))
			.placement(instance.getPlacement())
			.build();
		return saveCloudHost(cloudHost);
	}

	private CloudHost addCloudHostFromSimpleInstance(AwsSimpleInstance simpleInstance) {
		CloudHost cloudHost = CloudHost.builder()
			.instanceId(simpleInstance.getInstanceId())
			.instanceType(simpleInstance.getInstanceType())
			.state(simpleInstance.getState())
			.imageId(simpleInstance.getImageId())
			.publicDnsName(simpleInstance.getPublicDnsName())
			.publicIpAddress(simpleInstance.getPublicIpAddress())
			.privateIpAddress(simpleInstance.getPrivateIpAddress())
			.awsRegion(this.getPlacementRegion(simpleInstance.getPlacement()))
			.placement(simpleInstance.getPlacement())
			.build();
		return saveCloudHost(cloudHost);
	}

	public CloudHost launchInstance(Coordinates coordinates) {
		log.info("Looking for the best aws region to start a cloud instance close to {}", coordinates);
		List<AwsRegion> awsRegions = AwsRegion.getAwsRegions();
		awsRegions.sort((oneRegion, anotherRegion) -> {
			double oneDistance = oneRegion.getCoordinates().distanceTo(coordinates);
			double anotherDistance = anotherRegion.getCoordinates().distanceTo(coordinates);
			return Double.compare(oneDistance, anotherDistance);
		});
		AwsRegion awsRegion = awsRegions.get(0);
		log.info("{} {} is the closest aws region with a distance of {} km", awsRegion.getZone(), awsRegion.getName(),
			(int) awsRegion.getCoordinates().distanceTo(coordinates) / 1000);
		return launchInstance(awsRegion);
	}

	public CloudHost launchInstance(AwsRegion region) {
		launchingInstance = true;
		try {
			Instance instance = awsService.createInstance(region);
			CloudHost cloudHost = saveCloudHostFromInstance(instance);
			hostsService.addHost(instance.getPublicIpAddress(), NodeRole.WORKER);
			return cloudHost;
		}
		finally {
			launchingInstance = false;
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
			log.error(e.getMessage());
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
		InstanceState state = new InstanceState().withCode(AwsInstanceState.SHUTTING_DOWN.getCode()).withName(AwsInstanceState.SHUTTING_DOWN.getState());
		cloudHost.setState(state);
		cloudHost = cloudHosts.save(cloudHost);
		awsService.terminateInstance(cloudHost.getInstanceId(), cloudHost.getAwsRegion(), wait);
		cloudHosts.delete(cloudHost);
	}

	public void terminateInstances() {
		awsService.getInstances().parallelStream()
			.filter(instance -> !Objects.equals(instance.getState().getCode(), AwsInstanceState.TERMINATED.getCode()))
			.forEach(instance -> terminateInstance(instance.getInstanceId(), false));
	}

	public List<CloudHost> synchronizeDatabaseCloudHosts() {
		List<CloudHost> cloudHosts = getCloudHosts();
		if (launchingInstance) {
			return cloudHosts;
		}
		List<Instance> awsInstances = awsService.getInstances();
		Map<String, Instance> awsInstancesIds = awsInstances.stream()
			.collect(Collectors.toMap(Instance::getInstanceId, instance -> instance));
		Iterator<CloudHost> cloudHostsIterator = cloudHosts.iterator();
		// Remove invalid and update cloud host entities
		while (cloudHostsIterator.hasNext()) {
			CloudHost cloudHost = cloudHostsIterator.next();
			String instanceId = cloudHost.getInstanceId();
			if (!awsInstancesIds.containsKey(instanceId)) {
				this.cloudHosts.delete(cloudHost);
				cloudHostsIterator.remove();
				log.info("Removing invalid cloud host {}", instanceId);
			}
			else {
				Instance instance = awsInstancesIds.get(instanceId);
				InstanceState currentState = instance.getState();
				InstanceState savedState = cloudHost.getState();
				if (Objects.equals(currentState.getCode(), AwsInstanceState.TERMINATED.getCode())) {
					this.cloudHosts.delete(cloudHost);
					log.info("Removing terminated cloud host {}", instanceId);
				}
				else if (!Objects.equals(currentState, savedState)) {
					CloudHost newCloudHost = saveCloudHostFromInstance(cloudHost.getId(), instance);
					log.info("Updating state of cloud host {}", newCloudHost.getInstanceId());
				}
			}
		}
		// Add missing cloud host entities
		awsInstances.forEach(instance -> {
			String instanceId = instance.getInstanceId();
			if (instance.getState().getCode() != AwsInstanceState.TERMINATED.getCode() && !hasCloudHost(instanceId)) {
				CloudHost cloudHost = addCloudHostFromSimpleInstance(new AwsSimpleInstance(instance));
				cloudHosts.add(cloudHost);
				log.info("Added missing cloud host {}", instanceId);
			}
		});
		return cloudHosts;
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
		checkCloudHostExists(hostname);
		hostRulesService.removeCloudHost(ruleName, hostname);
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

	public void addSimulatedMetric(String instanceId, String simulatedMetricName) {
		checkCloudHostExists(instanceId);
		hostSimulatedMetricsService.addCloudHost(simulatedMetricName, instanceId);
	}

	public void addSimulatedMetrics(String instanceId, List<String> simulatedMetricNames) {
		checkCloudHostExists(instanceId);
		simulatedMetricNames.forEach(simulatedMetric ->
			hostSimulatedMetricsService.addCloudHost(simulatedMetric, instanceId));
	}

	public void removeSimulatedMetric(String instanceId, String simulatedMetricName) {
		checkCloudHostExists(instanceId);
		hostSimulatedMetricsService.addCloudHost(simulatedMetricName, instanceId);
	}

	public void removeSimulatedMetrics(String instanceId, List<String> simulatedMetricNames) {
		checkCloudHostExists(instanceId);
		simulatedMetricNames.forEach(simulatedMetric -> hostSimulatedMetricsService.addCloudHost(simulatedMetric, instanceId));
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

	public boolean hasCloudHost(String hostname) {
		return cloudHosts.hasCloudHost(hostname);
	}

	private void checkCloudHostExists(String hostname) {
		if (!hasCloudHost(hostname)) {
			throw new EntityNotFoundException(CloudHost.class, "hostname", hostname);
		}
	}

	private AwsRegion getPlacementRegion(Placement placement) {
		String availabilityZone = placement.getAvailabilityZone();
		while (!Character.isDigit(availabilityZone.charAt(availabilityZone.length() - 1))) {
			availabilityZone = availabilityZone.substring(0, availabilityZone.length() - 1);
		}
		AwsRegion region = AwsRegion.valueOf(availabilityZone.toUpperCase().replace("-", "_"));
		log.info("Instance placement {} is on aws region {}", placement.getAvailabilityZone(), region.name());
		return region;
	}

	public void initSyncDatabaseCloudHostsTimer() {
		syncDatabaseCloudHostsTimer = new Timer("SyncDatabaseCloudHostsTimer", true);
		syncDatabaseCloudHostsTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				try {
					synchronizeDatabaseCloudHosts();
				}
				catch (ManagerException e) {
					log.error(e.getMessage());
				}
			}
		}, CLOUD_HOSTS_DATABASE_SYNC_INTERVAL, CLOUD_HOSTS_DATABASE_SYNC_INTERVAL);
	}

	public void stopSyncDatabaseCloudHostsTimer() {
		if (syncDatabaseCloudHostsTimer != null) {
			syncDatabaseCloudHostsTimer.cancel();
			log.info("Stopped cloud hosts database synchronization");
		}
	}
}
