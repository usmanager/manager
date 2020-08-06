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

package pt.unl.fct.miei.usmanagement.manager.master.management.containers;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import net.minidev.json.JSONArray;
import org.springframework.data.util.Pair;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pt.unl.fct.miei.usmanagement.manager.database.containers.ContainerEntity;
import pt.unl.fct.miei.usmanagement.manager.database.monitoring.ContainerSimulatedMetricEntity;
import pt.unl.fct.miei.usmanagement.manager.database.rulesystem.rules.ContainerRuleEntity;
import pt.unl.fct.miei.usmanagement.manager.master.management.loadbalancer.nginx.NginxLoadBalancerService;
import pt.unl.fct.miei.usmanagement.manager.master.management.services.discovery.eureka.EurekaService;
import pt.unl.fct.miei.usmanagement.manager.master.util.Json;

@RestController
@RequestMapping("/containers")
public class ContainersController {

  //TODO substituir os reqs por objetos, ou @JsonValue annotation

  private final ContainersService containersService;
  private final EurekaService eurekaService;
  private final NginxLoadBalancerService nginxLoadBalancerService;

  public ContainersController(ContainersService containersService,
                              EurekaService eurekaService,
                              NginxLoadBalancerService nginxLoadBalancerService) {
    this.containersService = containersService;
    this.eurekaService = eurekaService;
    this.nginxLoadBalancerService = nginxLoadBalancerService;
  }

  @GetMapping
  public List<ContainerEntity> getContainers(@RequestParam(required = false) String serviceName) {
    List<ContainerEntity> containers;
    if (serviceName != null) {
      containers = containersService.getContainersWithLabels(
          Set.of(Pair.of(ContainerConstants.Label.SERVICE_NAME, serviceName)));
    } else {
      containers = containersService.getContainers();
    }
    return containers;
  }

  @GetMapping("/{id}")
  public ContainerEntity getContainer(@PathVariable String id) {
    return containersService.getContainer(id);
  }

  @PostMapping
  public ContainerEntity launchContainer(@Json String hostname, @Json String service,
                                         @Json String internalPort, @Json String externalPort) {
    return containersService.launchContainer(hostname, service, internalPort, externalPort);
  }

  @DeleteMapping("/{id}")
  public void deleteContainer(@PathVariable String id) {
    containersService.stopContainer(id);
  }

  @PostMapping("/{id}/replicate")
  public ContainerEntity replicateContainer(@PathVariable String id, @Json String hostname) {
    return containersService.replicateContainer(id, hostname);
  }

  @PostMapping("/{id}/migrate")
  public ContainerEntity migrateContainer(@PathVariable String id, @Json String hostname) {
    return containersService.migrateContainer(id, hostname);
  }

  @PostMapping("/load-balancer")
  public List<ContainerEntity> launchLoadBalancer(@Json String service, @Json JSONArray regions) {
    return nginxLoadBalancerService.launchLoadBalancers(service, regions.toArray(new String[0]));
  }

  //TODO
  @PostMapping("/eureka-server")
  public List<ContainerEntity> launchEureka(@Json JSONArray regions) {
    return eurekaService.launchEurekaServers(regions.toArray(new String[0]));
  }

  @PostMapping("/reload")
  public List<ContainerEntity> reloadContainers() {
    return containersService.reloadContainers();
  }

  @GetMapping("/{containerId}/logs")
  public String getContainerLogs(@PathVariable String containerId) {
    return containersService.getLogs(containerId);
  }

  @GetMapping("/{containerId}/rules")
  public List<ContainerRuleEntity> addContainerRule(@PathVariable String containerId) {
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
  public List<ContainerSimulatedMetricEntity> getContainerSimulatedMetrics(@PathVariable String containerId) {
    return containersService.getSimulatedMetrics(containerId);
  }

  @PostMapping("/{containerId}/simulated-metrics")
  public void addContainerSimulatedMetrics(@PathVariable String containerId, @RequestBody String[] simulatedMetrics) {
    containersService.addSimulatedMetrics(containerId, Arrays.asList(simulatedMetrics));
  }

  @DeleteMapping("/{containerId}/simulated-metrics")
  public void removeContainerSimulatedMetrics(@PathVariable String containerId,
                                              @RequestBody String[] simulatedMetrics) {
    containersService.removeSimulatedMetrics(containerId, Arrays.asList(simulatedMetrics));
  }

  @DeleteMapping("/{containerId}/simulated-metrics/{simulatedMetricName}")
  public void removeContainerSimulatedMetric(@PathVariable String containerId,
                                             @PathVariable String simulatedMetricName) {
    containersService.removeSimulatedMetric(containerId, simulatedMetricName);
  }

}