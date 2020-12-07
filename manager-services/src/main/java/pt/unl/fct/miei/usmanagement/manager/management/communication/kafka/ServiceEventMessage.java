package pt.unl.fct.miei.usmanagement.manager.management.communication.kafka;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import pt.unl.fct.miei.usmanagement.manager.monitoring.ServiceEvent;
import pt.unl.fct.miei.usmanagement.manager.rulesystem.decision.Decision;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
public class ServiceEventMessage {

	private Long id;
	private String containerId;
	private String serviceName;
	private String managerPublicIpAddress;
	private String managerPrivateIpAddress;
	private Decision decision;
	private int count;

	public ServiceEventMessage(ServiceEvent serviceEvent) {
		this.id = serviceEvent.getId();
		this.containerId = serviceEvent.getContainerId();
		this.managerPublicIpAddress = serviceEvent.getManagerPublicIpAddress();
		this.managerPrivateIpAddress = serviceEvent.getManagerPrivateIpAddress();
		this.decision = serviceEvent.getDecision();
		this.count = serviceEvent.getCount();
	}

	public ServiceEvent get() {
		return ServiceEvent.builder()
			.id(id)
			.serviceName(serviceName)
			.containerId(containerId)
			.managerPublicIpAddress(managerPublicIpAddress)
			.managerPrivateIpAddress(managerPrivateIpAddress)
			.decision(decision)
			.count(count)
			.build();
	}
}