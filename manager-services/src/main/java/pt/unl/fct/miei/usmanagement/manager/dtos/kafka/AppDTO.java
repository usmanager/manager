package pt.unl.fct.miei.usmanagement.manager.dtos.kafka;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@JsonIdentityInfo(generator = ObjectIdGenerators.UUIDGenerator.class, scope = AppDTO.class)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AppDTO {

	private Long id;
	private String name;
	private String description;
	private Set<AppServiceDTO> appServices;
	private Set<AppRuleDTO> appRules;
	private Set<AppSimulatedMetricDTO> simulatedAppMetrics;

	public AppDTO(Long id) {
		this.id = id;
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
		if (!(o instanceof AppDTO)) {
			return false;
		}
		AppDTO other = (AppDTO) o;
		return id != null && id.equals(other.getId());
	}

	@Override
	public String toString() {
		return "AppDTO{" +
			"id=" + id +
			", name='" + name + '\'' +
			", description='" + description + '\'' +
			", appServices=" + (appServices == null ? "null" : appServices.stream().map(AppServiceDTO::getId).collect(Collectors.toSet())) +
			", appRules=" + (appRules == null ? "null" : appRules.stream().map(AppRuleDTO::getId).collect(Collectors.toSet())) +
			", simulatedAppMetrics=" + (simulatedAppMetrics == null ? "null" : simulatedAppMetrics.stream().map(AppSimulatedMetricDTO::getId).collect(Collectors.toSet())) +
			'}';
	}
}
