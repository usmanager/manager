package pt.unl.fct.miei.usmanagement.manager.dtos.kafka;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@JsonIdentityInfo(generator = ObjectIdGenerators.UUIDGenerator.class, scope = ServiceDecisionValueDTO.class)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ServiceDecisionValueDTO {

	private Long id;
	private ServiceDecisionDTO serviceDecision;
	private FieldDTO field;
	private double value;

	@Override
	public int hashCode() {
		return Objects.hashCode(getId());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ServiceDecisionValueDTO)) {
			return false;
		}
		ServiceDecisionValueDTO other = (ServiceDecisionValueDTO) o;
		return id != null && id.equals(other.getId());
	}

	@Override
	public String toString() {
		return "ServiceDecisionValueDTO{" +
			"id=" + id +
			", serviceDecision=" + serviceDecision +
			", field=" + field +
			", value=" + value +
			'}';
	}
}
