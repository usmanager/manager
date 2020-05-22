/*
 * MIT License
 *
 * Copyright (c) 2020 usmanager
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

package pt.unl.fct.microservicemanagement.mastermanager.manager.docker.swarm.node;

import pt.unl.fct.microservicemanagement.mastermanager.manager.hosts.HostsService;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/nodes")
public class NodesController {

  private final NodesService nodesService;
  private final HostsService hostsService;

  public NodesController(NodesService nodesService, HostsService hostsService) {
    this.nodesService = nodesService;
    this.hostsService = hostsService;
  }

  @GetMapping
  public List<SimpleNode> getNodes() {
    return nodesService.getNodes();
  }

  @GetMapping("/{id}")
  public SimpleNode getNode(@PathVariable("id") String id) {
    return nodesService.getNode(id);
  }

  @PostMapping
  public void addNodes(@RequestBody AddNode addNode) {
    //TODO NodeRole role = addNode.getRole();
    NodeRole role = NodeRole.WORKER;
    int quantity = addNode.getQuantity();
    String hostname = addNode.getHostname();
    if (hostname != null) {
      for (var i = 0; i < quantity; i++) {
        hostsService.addHost(role, hostname);
      }
    } else {
      String region = addNode.getRegion().getName();
      String country = addNode.getCountry();
      String city = addNode.getCity();
      for (var i = 0; i < quantity; i++) {
        hostsService.addHost(role, region, country, city);
      }
    }
  }

  @DeleteMapping("/{id}")
  public void removeNode(@PathVariable("id") String id) {
    var node = nodesService.getNode(id);
    hostsService.removeHost(node.getHostname());
  }

}
