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

package pt.unl.fct.miei.usmanagement.manager.metrics.simulated;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Singular;
import org.hibernate.annotations.GenericGenerator;
import pt.unl.fct.miei.usmanagement.manager.containers.Container;
import pt.unl.fct.miei.usmanagement.manager.fields.Field;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

@Entity
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@NoArgsConstructor
@Setter
@Getter
@Table(name = "simulated_container_metrics")
public class ContainerSimulatedMetric /*extends AbstractEntity<Long> */ {

	@Id
	@GenericGenerator(name = "IdGenerator", strategy = "pt.unl.fct.miei.usmanagement.manager.IdGenerator")
	@GeneratedValue(generator = "IdGenerator")
	private Long id;

	@NotNull
	@Column(unique = true)
	private String name;

	@ManyToOne
	@JoinColumn(name = "field_id")
	private Field field;

	@Min(0)
	@NotNull
	private double minimumValue;

	@NotNull
	private double maximumValue;

	@NotNull
	private boolean override;

	@NotNull
	private boolean active;

	@Singular
	@JsonIgnore
	@ManyToMany(mappedBy = "simulatedContainerMetrics", fetch = FetchType.EAGER)
	private Set<Container> containers;

	public void addContainer(Container container) {
		containers.add(container);
		container.getSimulatedContainerMetrics().add(this);
	}

	public void removeContainer(Container container) {
		containers.remove(container);
		container.getSimulatedContainerMetrics().remove(this);
	}

	public void removeAssociations() {
		Iterator<Container> containersIterator = containers.iterator();
		while (containersIterator.hasNext()) {
			Container container = containersIterator.next();
			containersIterator.remove();
			container.getSimulatedContainerMetrics().remove(this);
		}
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
		if (!(o instanceof ContainerSimulatedMetric)) {
			return false;
		}
		ContainerSimulatedMetric other = (ContainerSimulatedMetric) o;
		return id != null && id.equals(other.getId());
	}

}
