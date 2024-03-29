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

package pt.unl.fct.miei.usmanagement.manager.management.containers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pt.unl.fct.miei.usmanagement.manager.Mode;
import pt.unl.fct.miei.usmanagement.manager.config.ManagerServicesConfiguration;
import pt.unl.fct.miei.usmanagement.manager.containers.Container;
import pt.unl.fct.miei.usmanagement.manager.containers.ContainerConstants;
import pt.unl.fct.miei.usmanagement.manager.hosts.Coordinates;
import pt.unl.fct.miei.usmanagement.manager.hosts.HostAddress;
import pt.unl.fct.miei.usmanagement.manager.metrics.simulated.ContainerSimulatedMetric;
import pt.unl.fct.miei.usmanagement.manager.nodes.Node;
import pt.unl.fct.miei.usmanagement.manager.rulesystem.rules.ContainerRule;
import pt.unl.fct.miei.usmanagement.manager.services.ServiceConstants;
import pt.unl.fct.miei.usmanagement.manager.services.containers.ContainersService;
import pt.unl.fct.miei.usmanagement.manager.services.containers.LaunchContainerRequest;
import pt.unl.fct.miei.usmanagement.manager.services.docker.nodes.NodesService;
import pt.unl.fct.miei.usmanagement.manager.services.hosts.HostsService;
import pt.unl.fct.miei.usmanagement.manager.services.workermanagers.WorkerManagersService;
import pt.unl.fct.miei.usmanagement.manager.sync.SyncService;

@Slf4j
@RestController
@RequestMapping("/containers")
public class ContainersController {

	private final ContainersService containersService;
	private final WorkerManagersService workerManagersService;
	private final SyncService syncService;
	private final HostsService hostsService;
	private final ManagerServicesConfiguration managerServicesConfiguration;
	private final NodesService nodesService;

	public ContainersController(ContainersService containersService, WorkerManagersService workerManagersService,
								SyncService syncService, HostsService hostsService,
								ManagerServicesConfiguration managerServicesConfiguration, NodesService nodesService) {
		this.containersService = containersService;
		this.workerManagersService = workerManagersService;
		this.syncService = syncService;
		this.hostsService = hostsService;
		this.managerServicesConfiguration = managerServicesConfiguration;
		this.nodesService = nodesService;
	}

	@GetMapping
	public List<Container> getContainers(@RequestParam(required = false) String serviceName) {
		List<Container> containers;
		if (serviceName != null) {
			containers = containersService.getContainersWithLabels(
				Set.of(Pair.of(ContainerConstants.Label.SERVICE_NAME, serviceName))
			);
		}
		else {
			containers = containersService.getContainers();
		}
		return containers;
	}

	@GetMapping("/{id}")
	public Container getContainer(@PathVariable String id) {
		return containersService.getContainer(id);
	}

	@PostMapping
	public List<Container> launchContainer(@RequestBody LaunchContainerRequest launchContainerRequest) {
		HostAddress hostAddress = launchContainerRequest.getHostAddress();
		String service = launchContainerRequest.getService();
		int internalPort = launchContainerRequest.getInternalPort();
		int externalPort = launchContainerRequest.getExternalPort();
		if (managerServicesConfiguration.getMode() == Mode.LOCAL) {
			if (hostAddress == null) {
				hostAddress = hostsService.getManagerHostAddress();
			}
			return List.of(containersService.launchContainer(hostAddress, service, internalPort, externalPort));
		}
		if (hostAddress != null) {
			if (hostsService.getManagerHostAddress().equals(hostAddress)) {
			 	return List.of(containersService.launchContainer(hostAddress, service, internalPort, externalPort));
			}
			else {
				List<Node> nodes = nodesService.getHostNodes(hostAddress);
					if (nodes.size() > 0) {
						Node node = nodes.get(0);
						if (node.getManagerId().equalsIgnoreCase(ServiceConstants.Name.MASTER_MANAGER)) {
							return List.of(containersService.launchContainer(hostAddress, service, internalPort, externalPort));
						}
					}
				return workerManagersService.launchContainers(launchContainerRequest);
			}
		}
		else {
			boolean isWorkerManager = launchContainerRequest.isWorkerManager();
			if (isWorkerManager) {
				return workerManagersService.launchContainers(launchContainerRequest);
			}
			List<Container> containers = new ArrayList<>();
			List<Coordinates> coordinates = launchContainerRequest.getCoordinates();
			for (Coordinates c : coordinates) {
				Container container = containersService.launchContainer(c, service, internalPort, externalPort);
				containers.add(container);
			}
			return containers;
		}
	}

	@DeleteMapping("/{id}")
	public void deleteContainer(@PathVariable String id) {
		containersService.stopContainer(id);
	}

	@PostMapping("/{id}/replicate")
	public Container replicateContainer(@PathVariable String id, @RequestBody HostAddress hostAddress) {
		return containersService.replicateContainer(id, hostAddress);
	}

	@PostMapping("/{id}/migrate")
	public Container migrateContainer(@PathVariable String id, @RequestBody HostAddress hostAddress) {
		return containersService.migrateContainer(id, hostAddress);
	}

	@PostMapping("/sync")
	public List<Container> syncDatabaseContainers() {
		List<Container> workerContainers = workerManagersService.synchronizeDatabaseContainers();
		List<Container> containers = syncService.synchronizeContainersDatabase();
		containers.addAll(workerContainers);
		return containers;
	}

	@GetMapping("/{containerId}/logs")
	public String getContainerLogs(@PathVariable String containerId) {
		return containersService.getLogs(containerId);
	}

	@GetMapping("/{containerId}/rules")
	public List<ContainerRule> addContainerRule(@PathVariable String containerId) {
		return containersService.getRules(containerId);
	}

	@PostMapping("/{containerId}/rules")
	public void addContainerRules(@PathVariable String containerId, @RequestBody String[] rules) {
		containersService.addRules(containerId, Arrays.asList(rules));
	}

	@DeleteMapping("/{containerId}/rules")
	public void removeContainerRules(@PathVariable String containerId, @RequestBody String[] rules) {
		containersService.removeRules(containerId, Arrays.asList(rules));
	}

	@DeleteMapping("/{containerId}/rules/{ruleName}")
	public void removeContainerRule(@PathVariable String containerId, @PathVariable String ruleName) {
		containersService.removeRule(containerId, ruleName);
	}

	@GetMapping("/{containerId}/simulated-metrics")
	public List<ContainerSimulatedMetric> getContainerSimulatedMetrics(@PathVariable String containerId) {
		return containersService.getSimulatedMetrics(containerId);
	}

	@PostMapping("/{containerId}/simulated-metrics")
	public void addContainerSimulatedMetrics(@PathVariable String containerId, @RequestBody String[] simulatedMetrics) {
		containersService.addSimulatedMetrics(containerId, Arrays.asList(simulatedMetrics));
	}

	@DeleteMapping("/{containerId}/simulated-metrics")
	public void removeContainerSimulatedMetrics(@PathVariable String containerId, @RequestBody String[] simulatedMetrics) {
		containersService.removeSimulatedMetrics(containerId, Arrays.asList(simulatedMetrics));
	}

	@DeleteMapping("/{containerId}/simulated-metrics/{simulatedMetricName}")
	public void removeContainerSimulatedMetric(@PathVariable String containerId, @PathVariable String simulatedMetricName) {
		containersService.removeSimulatedMetric(containerId, simulatedMetricName);
	}

}
