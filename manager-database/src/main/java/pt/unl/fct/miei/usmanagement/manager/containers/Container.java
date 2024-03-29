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

package pt.unl.fct.miei.usmanagement.manager.containers;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Singular;
import lombok.ToString;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.NaturalId;
import pt.unl.fct.miei.usmanagement.manager.hosts.Coordinates;
import pt.unl.fct.miei.usmanagement.manager.hosts.HostAddress;
import pt.unl.fct.miei.usmanagement.manager.metrics.simulated.ContainerSimulatedMetric;
import pt.unl.fct.miei.usmanagement.manager.regions.RegionEnum;
import pt.unl.fct.miei.usmanagement.manager.rulesystem.rules.ContainerRule;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.MapKeyColumn;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Entity
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@NoArgsConstructor
@Setter
@Getter
@ToString(exclude = {"containerRules", "simulatedContainerMetrics"})
@JsonPropertyOrder({ "id", "type", "created", "name", "image", "command", "network", "publicIpAddress", "privateIpAddress", "mounts",
		"ports", "labels", "region", "state", "managerId", "coordinates"})
@Table(name = "containers")
public class Container /*extends AbstractEntity<String> */ {

	@Id
	@NaturalId
	private String id;

	@NotNull
	private ContainerTypeEnum type;

	@NotNull
	private long created;

	@NotNull
	private String name;

	@NotNull
	private String image;

	@Column(length = 516)
	private String command;

	private String network;

	@NotNull
	private String publicIpAddress;

	@NotNull
	private String privateIpAddress;

	@ElementCollection(fetch = FetchType.EAGER)
	@Fetch(value = FetchMode.SUBSELECT)
	private Set<String> mounts;

	@ElementCollection(fetch = FetchType.EAGER)
	@Fetch(value = FetchMode.SUBSELECT)
	private Set<ContainerPortMapping> ports;

	@MapKeyColumn(name = "LABEL_KEY", length = 64)
	@Column(name = "LABEL_VALUE", length = 2048)
	@ElementCollection(fetch = FetchType.EAGER)
	@Fetch(value = FetchMode.SUBSELECT)
	private Map<String, String> labels;

	@NotNull
	private RegionEnum region;

	@NotNull
	private String state;

	@NotNull
	private Coordinates coordinates;

	@Singular
	@JsonIgnore
	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "container_rules",
		joinColumns = @JoinColumn(name = "container_id"),
		inverseJoinColumns = @JoinColumn(name = "rule_id")
	)
	private Set<ContainerRule> containerRules;

	@Singular
	@JsonIgnore
	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "container_simulated_metrics",
		joinColumns = @JoinColumn(name = "container_id"),
		inverseJoinColumns = @JoinColumn(name = "simulated_metric_id")
	)
	private Set<ContainerSimulatedMetric> simulatedContainerMetrics;

	public String getManagerId() {
		return getLabels().get(ContainerConstants.Label.MANAGER_ID);
	}

	@JsonIgnore
	public String getServiceName() {
		return getLabels().get(ContainerConstants.Label.SERVICE_NAME);
	}

	@JsonIgnore
	public String getAddress() {
		return String.format("%s:%s", publicIpAddress, labels.get(ContainerConstants.Label.SERVICE_PORT));
	}

	public void addRule(ContainerRule rule) {
		containerRules.add(rule);
		rule.getContainers().add(this);
	}

	public void removeRule(ContainerRule rule) {
		containerRules.remove(rule);
		rule.getContainers().remove(this);
	}

	public void addContainerSimulatedMetric(ContainerSimulatedMetric containerMetric) {
		simulatedContainerMetrics.add(containerMetric);
		containerMetric.getContainers().add(this);
	}

	public void removeContainerSimulatedMetric(ContainerSimulatedMetric containerMetric) {
		simulatedContainerMetrics.remove(containerMetric);
		containerMetric.getContainers().remove(this);
	}

	public void clearAssociations() {
		if (containerRules != null) {
			containerRules.clear();
		}
		if (simulatedContainerMetrics != null) {
			simulatedContainerMetrics.clear();
		}
	}

	@JsonIgnore
	public HostAddress getHostAddress() {
		return new HostAddress(publicIpAddress, privateIpAddress, coordinates, region);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(getId());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Container)) {
			return false;
		}
		Container other = (Container) o;
		return id != null && id.equals(other.getId());
	}
}
