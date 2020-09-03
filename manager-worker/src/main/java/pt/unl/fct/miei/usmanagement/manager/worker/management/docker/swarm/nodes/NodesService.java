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

package pt.unl.fct.miei.usmanagement.manager.worker.management.docker.swarm.nodes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.swarm.Node;
import com.spotify.docker.client.messages.swarm.NodeInfo;
import com.spotify.docker.client.messages.swarm.NodeSpec;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pt.unl.fct.miei.usmanagement.manager.database.hosts.cloud.CloudHostEntity;
import pt.unl.fct.miei.usmanagement.manager.database.hosts.edge.EdgeHostEntity;
import pt.unl.fct.miei.usmanagement.manager.database.workermanagers.WorkerManagerEntity;
import pt.unl.fct.miei.usmanagement.manager.worker.exceptions.EntityNotFoundException;
import pt.unl.fct.miei.usmanagement.manager.worker.exceptions.WorkerManagerException;
import pt.unl.fct.miei.usmanagement.manager.worker.management.docker.swarm.DockerSwarmService;
import pt.unl.fct.miei.usmanagement.manager.worker.management.hosts.HostsService;
import pt.unl.fct.miei.usmanagement.manager.worker.management.hosts.cloud.CloudHostsService;
import pt.unl.fct.miei.usmanagement.manager.worker.management.hosts.edge.EdgeHostsService;

@Service
@Slf4j
public class NodesService {

  private final DockerSwarmService dockerSwarmService;
  private final CloudHostsService cloudHostsService;
  private final EdgeHostsService edgeHostsService;
  private final HostsService hostsService;

  @Value("${external-id}")
  private String workerManagerId;

  public NodesService(DockerSwarmService dockerSwarmService, CloudHostsService cloudHostsService,
                      EdgeHostsService edgeHostsService, HostsService hostsService) {
    this.dockerSwarmService = dockerSwarmService;
    this.cloudHostsService = cloudHostsService;
    this.edgeHostsService = edgeHostsService;
    this.hostsService = hostsService;
  }

  public List<SimpleNode> getNodes() {
    return getNodes(null);
  }

  private List<SimpleNode> getNodes(Predicate<Node> filter) {
    try (var swarmManager = dockerSwarmService.getSwarmLeader()) {
      Stream<Node> nodeStream = swarmManager.listNodes().stream();
      if (filter != null) {
        nodeStream = nodeStream.filter(filter);
      }
      return nodeStream
          .map(n -> new SimpleNode(n.id(), n.status().addr(),
              NodeAvailability.valueOf(n.spec().availability().toUpperCase()),
              NodeRole.valueOf(n.spec().role().toUpperCase()), n.version().index(), n.spec().labels(),
              n.status().state()))
          .collect(Collectors.toList());
    } catch (DockerException | InterruptedException e) {
      throw new WorkerManagerException(e.getMessage());
    }
  }

  public SimpleNode getNode(String id) {
    return getNodes(node -> Objects.equals(node.id(), id)).stream()
        .findFirst()
        .orElseThrow(() -> new EntityNotFoundException(SimpleNode.class, "id", id));
  }

  public SimpleNode getHostNode(String hostname) {
    return getNodes(node -> Objects.equals(node.status().addr(), hostname)).stream()
        .findFirst()
        .orElseThrow(() -> new EntityNotFoundException(SimpleNode.class, "hostname", hostname));
  }

  public List<SimpleNode> getActiveNodes() {
    return getActiveNodes(null);
  }

  public List<SimpleNode> getActiveNodes(Predicate<Node> filter) {
    Predicate<Node> activeFilter = n -> n.spec().availability().equalsIgnoreCase("active");
    Predicate<Node> nodesFilter = filter == null ? activeFilter : filter.and(activeFilter);
    return getReadyNodes(nodesFilter);
  }

  public List<SimpleNode> getReadyNodes() {
    return getReadyNodes(null);
  }

  public List<SimpleNode> getReadyNodes(Predicate<Node> filter) {
    Predicate<Node> readyFilter = n -> n.status().state().equals("ready");
    Predicate<Node> nodesFilter = filter == null ? readyFilter : filter.and(readyFilter);
    return getNodes(nodesFilter);
  }

  public List<SimpleNode> getReadyManagers() {
    return getReadyNodes(node -> node.managerStatus() != null);
  }

  public List<SimpleNode> getReadyWorkers() {
    return getReadyNodes(node -> node.managerStatus() == null);
  }

  private void removeNodes(Predicate<Node> filter) {
    getNodes(filter).forEach(n -> removeNode(n.getId()));
  }

  public void removeHostNodes(String hostname) {
    removeNodes(n -> Objects.equals(n.status().addr(), hostname));
  }

  public void removeNode(String nodeId) {
    //var node = getNode(nodeId);
    try (var swarmManager = dockerSwarmService.getSwarmLeader()) {
      swarmManager.deleteNode(nodeId);
      /*int hostNodes = getNodes(n -> Objects.equals(n.status().addr(), node.getHostname())).size();
      if (hostNodes == 0) {
        hostsService.removeHost(node.getHostname());
      }*/
      log.info("Deleted node '{}'", nodeId);
    } catch (DockerException | InterruptedException e) {
      e.printStackTrace();
      throw new WorkerManagerException(e.getMessage());
    }
  }

  public boolean isPartOfSwarm(String hostname) {
    return getNodes(n -> Objects.equals(n.status().addr(), hostname)).size() > 0;
  }

  public boolean isManager(String nodeId) {
    try (var swarmManager = dockerSwarmService.getSwarmLeader()) {
      NodeInfo nodeInfo = swarmManager.inspectNode(nodeId);
      return nodeInfo.managerStatus() != null;
    } catch (DockerException | InterruptedException e) {
      e.printStackTrace();
      throw new WorkerManagerException(e.getMessage());
    }
  }

  public boolean isWorker(String nodeId) {
    try (var swarmManager = dockerSwarmService.getSwarmLeader()) {
      NodeInfo nodeInfo = swarmManager.inspectNode(nodeId);
      return nodeInfo.managerStatus() == null;
    } catch (DockerException | InterruptedException e) {
      e.printStackTrace();
      throw new WorkerManagerException(e.getMessage());
    }
  }

  public SimpleNode changeAvailability(String nodeId, NodeAvailability newAvailability) {
    SimpleNode node = getNode(nodeId);
    NodeSpec nodeSpec = NodeSpec.builder()
        .availability(newAvailability.name())
        .role(node.getRole().name())
        .build();
    return updateNode(node, nodeSpec);
  }

  public SimpleNode changeRole(String nodeId, NodeRole newRole) {
    SimpleNode node = getNode(nodeId);
    NodeSpec nodeSpec = NodeSpec.builder()
        .availability(node.getAvailability().name())
        .role(newRole.name())
        .build();
    return updateNode(node, nodeSpec);
  }

  public SimpleNode addLabel(String nodeId, String label, String value) {
    log.info("Adding label {}={} to node {}", label, value, nodeId);
    SimpleNode node = getNode(nodeId);
    NodeSpec nodeSpec = NodeSpec.builder()
        .availability(node.getAvailability().name())
        .role(node.getRole().name())
        .addLabel(label, value)
        .build();
    return updateNode(node, nodeSpec);
  }

  public SimpleNode removeLabel(String nodeId, String label) {
    log.info("Removing label {} from node {}", label, nodeId);
    SimpleNode node = getNode(nodeId);
    Map<String, String> labels = new HashMap<>(node.getLabels());
    labels.remove(label);
    NodeSpec nodeSpec = NodeSpec.builder()
        .availability(node.getAvailability().name())
        .role(node.getRole().name())
        .labels(labels)
        .build();
    return updateNode(node, nodeSpec);
  }

  public SimpleNode updateNode(String nodeId, SimpleNode newNode) {
    SimpleNode node = getNode(nodeId);
    log.info("Updating node {} to node {}", ToStringBuilder.reflectionToString(node),
        ToStringBuilder.reflectionToString(newNode));
    NodeSpec nodeSpec = NodeSpec.builder()
        .availability(newNode.getAvailability().name().toLowerCase())
        .role(newNode.getRole().name().toLowerCase())
        .labels(newNode.getLabels())
        .build();
    return updateNode(node, nodeSpec);
  }

  private SimpleNode updateNode(SimpleNode node, NodeSpec nodeSpec) {
    try (var swarmManager = dockerSwarmService.getSwarmLeader()) {
      String nodeId = node.getId();
      swarmManager.updateNode(nodeId, node.getVersion(), nodeSpec);
      return getNode(nodeId);
    } catch (DockerException | InterruptedException e) {
      e.printStackTrace();
      throw new WorkerManagerException(e.getMessage());
    }
  }

}