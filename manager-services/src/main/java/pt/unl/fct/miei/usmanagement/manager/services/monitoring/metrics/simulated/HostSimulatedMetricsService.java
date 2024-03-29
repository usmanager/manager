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

package pt.unl.fct.miei.usmanagement.manager.services.monitoring.metrics.simulated;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.unl.fct.miei.usmanagement.manager.exceptions.EntityNotFoundException;
import pt.unl.fct.miei.usmanagement.manager.hosts.HostAddress;
import pt.unl.fct.miei.usmanagement.manager.hosts.cloud.CloudHost;
import pt.unl.fct.miei.usmanagement.manager.hosts.edge.EdgeHost;
import pt.unl.fct.miei.usmanagement.manager.metrics.simulated.HostSimulatedMetric;
import pt.unl.fct.miei.usmanagement.manager.metrics.simulated.HostSimulatedMetrics;
import pt.unl.fct.miei.usmanagement.manager.services.communication.kafka.KafkaService;
import pt.unl.fct.miei.usmanagement.manager.services.hosts.cloud.CloudHostsService;
import pt.unl.fct.miei.usmanagement.manager.services.hosts.edge.EdgeHostsService;
import pt.unl.fct.miei.usmanagement.manager.util.EntityUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

@Slf4j
@Service
public class HostSimulatedMetricsService {

	private final CloudHostsService cloudHostsService;
	private final EdgeHostsService edgeHostsService;
	private final KafkaService kafkaService;

	private final HostSimulatedMetrics hostSimulatedMetrics;

	public HostSimulatedMetricsService(CloudHostsService cloudHostsService, EdgeHostsService edgeHostsService,
									   KafkaService kafkaService, HostSimulatedMetrics hostSimulatedMetrics) {
		this.cloudHostsService = cloudHostsService;
		this.edgeHostsService = edgeHostsService;
		this.kafkaService = kafkaService;
		this.hostSimulatedMetrics = hostSimulatedMetrics;
	}

	@Transactional(readOnly = true)
	public List<HostSimulatedMetric> getHostSimulatedMetrics() {
		return hostSimulatedMetrics.findAll();
	}

	public List<HostSimulatedMetric> getHostSimulatedMetricByHost(HostAddress hostAddress) {
		return hostSimulatedMetrics.findByHost(hostAddress.getPublicIpAddress(), hostAddress.getPrivateIpAddress());
	}

	public HostSimulatedMetric getHostSimulatedMetric(Long id) {
		return hostSimulatedMetrics.findById(id).orElseThrow(() ->
			new EntityNotFoundException(HostSimulatedMetric.class, "id", id.toString()));
	}

	public HostSimulatedMetric getHostSimulatedMetric(String simulatedMetricName) {
		return hostSimulatedMetrics.findByNameIgnoreCase(simulatedMetricName).orElseThrow(() ->
			new EntityNotFoundException(HostSimulatedMetric.class, "simulatedMetricName", simulatedMetricName));
	}

	public HostSimulatedMetric addHostSimulatedMetric(HostSimulatedMetric hostSimulatedMetric) {
		checkHostSimulatedMetricDoesntExist(hostSimulatedMetric);
		hostSimulatedMetric = saveHostSimulatedMetric(hostSimulatedMetric);
		/*HostSimulatedMetric kafkaHostSimulatedMetric = hostSimulatedMetric;
		kafkaHostSimulatedMetric.setNew(true);
		kafkaService.sendHostSimulatedMetric(kafkaHostSimulatedMetric);*/
		kafkaService.sendHostSimulatedMetric(hostSimulatedMetric);
		return hostSimulatedMetric;
	}

	public HostSimulatedMetric updateHostSimulatedMetric(String simulatedMetricName,
														 HostSimulatedMetric newHostSimulatedMetric) {
		log.info("Updating simulated host metric {} with {}", simulatedMetricName,
			ToStringBuilder.reflectionToString(newHostSimulatedMetric));
		HostSimulatedMetric hostSimulatedMetric = getHostSimulatedMetric(simulatedMetricName);
		EntityUtils.copyValidProperties(newHostSimulatedMetric, hostSimulatedMetric);
		hostSimulatedMetric = saveHostSimulatedMetric(hostSimulatedMetric);
		kafkaService.sendHostSimulatedMetric(hostSimulatedMetric);
		return hostSimulatedMetric;
	}

	public HostSimulatedMetric saveHostSimulatedMetric(HostSimulatedMetric simulatedMetric) {
		log.info("Saving hostSimulatedMetric {}", ToStringBuilder.reflectionToString(simulatedMetric));
		return hostSimulatedMetrics.save(simulatedMetric);
	}

	public HostSimulatedMetric addOrUpdateSimulatedMetric(HostSimulatedMetric simulatedMetric) {
		if (simulatedMetric.getId() != null) {
			Optional<HostSimulatedMetric> simulatedMetricOptional = hostSimulatedMetrics.findById(simulatedMetric.getId());
			if (simulatedMetricOptional.isPresent()) {
				HostSimulatedMetric existingSimulatedMetric = simulatedMetricOptional.get();
				Set<CloudHost> cloudHosts = simulatedMetric.getCloudHosts();
				if (cloudHosts != null) {
					Set<CloudHost> currentCloudHosts = existingSimulatedMetric.getCloudHosts();
					if (currentCloudHosts == null) {
						existingSimulatedMetric.setCloudHosts(new HashSet<>(cloudHosts));
					}
					else {
						cloudHosts.iterator().forEachRemaining(cloudHost -> {
							if (!currentCloudHosts.contains(cloudHost)) {
								cloudHost.addHostSimulatedMetric(simulatedMetric);
							}
						});
						currentCloudHosts.iterator().forEachRemaining(currentCloudHost -> {
							if (!cloudHosts.contains(currentCloudHost)) {
								currentCloudHost.removeHostSimulatedMetric(simulatedMetric);
							}
						});
					}
				}
				Set<EdgeHost> edgeHosts = simulatedMetric.getEdgeHosts();
				if (edgeHosts != null) {
					Set<EdgeHost> currentEdgeHosts = existingSimulatedMetric.getEdgeHosts();
					if (currentEdgeHosts == null) {
						existingSimulatedMetric.setEdgeHosts(new HashSet<>(edgeHosts));
					}
					else {
						edgeHosts.iterator().forEachRemaining(edgeHost -> {
							if (!currentEdgeHosts.contains(edgeHost)) {
								edgeHost.addHostSimulatedMetric(existingSimulatedMetric);
							}
						});
						currentEdgeHosts.iterator().forEachRemaining(currentEdgeHost -> {
							if (!edgeHosts.contains(currentEdgeHost)) {
								currentEdgeHost.removeHostSimulatedMetric(existingSimulatedMetric);
							}
						});
					}
				}
				EntityUtils.copyValidProperties(simulatedMetric, existingSimulatedMetric);
				return saveHostSimulatedMetric(existingSimulatedMetric);
			}
		}
		return saveHostSimulatedMetric(simulatedMetric);
	}

	public void deleteHostSimulatedMetric(Long id) {
		log.info("Deleting simulated host metric {}", id);
		HostSimulatedMetric simulatedHostMetric = getHostSimulatedMetric(id);
		deleteHostSimulatedMetric(simulatedHostMetric, false);
	}

	public void deleteHostSimulatedMetric(String simulatedMetricName) {
		log.info("Deleting simulated host metric {}", simulatedMetricName);
		HostSimulatedMetric simulatedHostMetric = getHostSimulatedMetric(simulatedMetricName);
		deleteHostSimulatedMetric(simulatedHostMetric, true);
	}

	public void deleteHostSimulatedMetric(HostSimulatedMetric simulatedMetric, boolean kafka) {
		simulatedMetric.removeAssociations();
		hostSimulatedMetrics.delete(simulatedMetric);
		if (kafka) {
			kafkaService.sendDeleteHostSimulatedMetric(simulatedMetric);
		}
	}

	public List<HostSimulatedMetric> getGenericHostSimulatedMetrics() {
		return hostSimulatedMetrics.findGenericHostSimulatedMetrics();
	}

	public HostSimulatedMetric getGenericHostSimulatedMetric(String simulatedMetricName) {
		return hostSimulatedMetrics.findGenericHostSimulatedMetric(simulatedMetricName).orElseThrow(() ->
			new EntityNotFoundException(HostSimulatedMetric.class, "simulatedMetricName", simulatedMetricName));
	}

	public List<CloudHost> getCloudHosts(String simulatedMetricName) {
		checkHostSimulatedMetricExists(simulatedMetricName);
		return hostSimulatedMetrics.getCloudHosts(simulatedMetricName);
	}

	public CloudHost getCloudHost(String simulatedMetricName, String instanceId) {
		checkHostSimulatedMetricExists(simulatedMetricName);
		return hostSimulatedMetrics.getCloudHost(simulatedMetricName, instanceId).orElseThrow(() ->
			new EntityNotFoundException(CloudHost.class, "instanceId", instanceId));
	}

	public void addCloudHost(String simulatedMetricName, String instanceId) {
		addCloudHosts(simulatedMetricName, List.of(instanceId));
	}

	public void addCloudHosts(String simulatedMetricName, List<String> instanceIds) {
		log.info("Adding cloud hosts {} to simulated metric {}", instanceIds, simulatedMetricName);
		HostSimulatedMetric hostMetric = getHostSimulatedMetric(simulatedMetricName);
		instanceIds.forEach(instanceId -> {
			CloudHost cloudHost = cloudHostsService.getCloudHostByIdOrIp(instanceId);
			cloudHost.addHostSimulatedMetric(hostMetric);
		});
		kafkaService.sendHostSimulatedMetric(hostSimulatedMetrics.save(hostMetric));
	}

	public void removeCloudHost(String simulatedMetricName, String instanceId) {
		removeCloudHosts(simulatedMetricName, List.of(instanceId));
	}

	public void removeCloudHosts(String simulatedMetricName, List<String> instanceIds) {
		log.info("Removing cloud hosts {} from simulated metric {}", instanceIds, simulatedMetricName);
		HostSimulatedMetric hostMetric = getHostSimulatedMetric(simulatedMetricName);
		instanceIds.forEach(instanceId -> cloudHostsService.getCloudHostByIdOrIp(instanceId).removeHostSimulatedMetric(hostMetric));
		kafkaService.sendHostSimulatedMetric(hostSimulatedMetrics.save(hostMetric));
	}

	public List<EdgeHost> getEdgeHosts(String simulatedMetricName) {
		checkHostSimulatedMetricExists(simulatedMetricName);
		return hostSimulatedMetrics.getEdgeHosts(simulatedMetricName);
	}

	public EdgeHost getEdgeHost(String simulatedMetricName, String hostname) {
		checkHostSimulatedMetricExists(simulatedMetricName);
		return hostSimulatedMetrics.getEdgeHost(simulatedMetricName, hostname).orElseThrow(() ->
			new EntityNotFoundException(EdgeHost.class, "hostname", hostname));
	}

	public void addEdgeHost(String simulatedMetricName, HostAddress hostAddress) {
		addEdgeHosts(simulatedMetricName, List.of(hostAddress));
	}

	public void addEdgeHosts(String simulatedMetricName, List<HostAddress> hostAddresses) {
		log.info("Adding edge hosts {} to simulated metric {}", hostAddresses, simulatedMetricName);
		HostSimulatedMetric hostMetric = getHostSimulatedMetric(simulatedMetricName);
		hostAddresses.forEach(hostAddress -> {
			EdgeHost edgeHost = edgeHostsService.getEdgeHostByAddress(hostAddress);
			edgeHost.addHostSimulatedMetric(hostMetric);
		});
		kafkaService.sendHostSimulatedMetric(hostSimulatedMetrics.save(hostMetric));
	}

	public void removeEdgeHost(String simulatedMetricName, HostAddress hostAddress) {
		removeEdgeHosts(simulatedMetricName, List.of(hostAddress));
	}

	public void removeEdgeHosts(String simulatedMetricName, List<HostAddress> hostAddresses) {
		log.info("Removing edge hosts {} from simulated metric {}", hostAddresses, simulatedMetricName);
		HostSimulatedMetric hostMetric = getHostSimulatedMetric(simulatedMetricName);
		hostAddresses.forEach(hostAddress -> edgeHostsService.getEdgeHostByAddress(hostAddress).removeHostSimulatedMetric(hostMetric));
		kafkaService.sendHostSimulatedMetric(hostSimulatedMetrics.save(hostMetric));
	}

	public Double randomizeFieldValue(HostSimulatedMetric metric) {
		Random random = new Random();
		double minValue = metric.getMinimumValue();
		double maxValue = metric.getMaximumValue();
		return minValue + (maxValue - minValue) * random.nextDouble();
	}

	private void checkHostSimulatedMetricExists(String simulatedMetricName) {
		if (!hostSimulatedMetrics.hasHostSimulatedMetric(simulatedMetricName)) {
			throw new EntityNotFoundException(HostSimulatedMetric.class, "simulatedMetricName", simulatedMetricName);
		}
	}

	private void checkHostSimulatedMetricDoesntExist(HostSimulatedMetric simulatedHostMetric) {
		String name = simulatedHostMetric.getName();
		if (hostSimulatedMetrics.hasHostSimulatedMetric(name)) {
			throw new DataIntegrityViolationException("Simulated host metric '" + name + "' already exists");
		}
	}
}
