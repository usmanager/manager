package pt.unl.fct.miei.usmanagement.manager.zookeeper;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pt.unl.fct.miei.usmanagement.manager.containers.Container;
import pt.unl.fct.miei.usmanagement.manager.regions.RegionEnum;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.util.Objects;

@Entity
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@NoArgsConstructor
@Setter
@Getter
@Table(name = "zookeepers")
public class Zookeeper /*extends AbstractEntity<String> */ {

	@Id
	private String id;

	@NotNull
	@OneToOne
	private Container container;

	@NotNull
	private RegionEnum region;

	@PrePersist
	private void prePersist() {
		if (this.getId() == null) {
			this.setId(container.getId());
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
		if (!(o instanceof Zookeeper)) {
			return false;
		}
		Zookeeper other = (Zookeeper) o;
		return id != null && id.equals(other.getId());
	}

}
