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

@JsonIdentityInfo(generator = ObjectIdGenerators.UUIDGenerator.class, scope = ServiceDTO.class)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ServiceRuleDTO {

	private Long id;
	private String name;
	private int priority;
	private boolean generic;
	private DecisionDTO decision;
	private Set<ServiceDTO> services;
	private Set<ServiceRuleConditionDTO> conditions;

	public ServiceRuleDTO(Long id) {
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
		if (!(o instanceof ServiceRuleDTO)) {
			return false;
		}
		ServiceRuleDTO other = (ServiceRuleDTO) o;
		return id != null && id.equals(other.getId());
	}

	@Override
	public String toString() {
		return "ServiceRuleDTO{" +
			"id=" + id +
			", name='" + name + '\'' +
			", priority=" + priority +
			", generic=" + generic +
			", decision=" + decision +
			", services=" + (services == null ? "null" : services.stream().map(ServiceDTO::getServiceName).collect(Collectors.toSet())) +
			", conditions=" + (conditions == null ? "null" : conditions.stream().map(ServiceRuleConditionDTO::getId).collect(Collectors.toSet())) +
			'}';
	}
}
