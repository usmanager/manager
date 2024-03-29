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

package pt.unl.fct.miei.usmanagement.manager.services.monitoring.events;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.springframework.stereotype.Service;
import pt.unl.fct.miei.usmanagement.manager.hosts.HostAddress;
import pt.unl.fct.miei.usmanagement.manager.monitoring.HostEvent;
import pt.unl.fct.miei.usmanagement.manager.monitoring.HostEvents;
import pt.unl.fct.miei.usmanagement.manager.rulesystem.decision.Decision;
import pt.unl.fct.miei.usmanagement.manager.services.hosts.HostsService;
import pt.unl.fct.miei.usmanagement.manager.services.rulesystem.decision.DecisionsService;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
public class HostsEventsService {

	private final HostEvents hostEvents;
	private final DecisionsService decisionsService;
	private final HostsService hostsService;

	public HostsEventsService(HostEvents hostEvents, DecisionsService decisionsService, HostsService hostsService) {
		this.hostEvents = hostEvents;
		this.decisionsService = decisionsService;
		this.hostsService = hostsService;
	}

	public List<HostEvent> getHostEvents() {
		return hostEvents.findAll();
	}

	public List<HostEvent> getHostEventsByHostAddress(HostAddress hostAddress) {
		return hostEvents.findByPublicIpAddressAndPrivateIpAddress(hostAddress.getPublicIpAddress(), hostAddress.getPrivateIpAddress());
	}

	public HostEvent addHostEvent(HostEvent hostEvent) {
		log.info("Saving host event: {}", ToStringBuilder.reflectionToString(hostEvent));
		return hostEvents.save(hostEvent);
	}

	public HostEvent saveHostEvent(HostAddress hostAddress, String decisionName) {
		HostAddress managerHostAddress = hostsService.getManagerHostAddress();
		Decision decision = decisionsService.getHostPossibleDecision(decisionName);
		HostEvent hostEvent = getHostEventsByHostAddress(hostAddress).stream().findFirst()
			.orElseGet(() -> HostEvent.builder().publicIpAddress(hostAddress.getPublicIpAddress()).privateIpAddress(hostAddress.getPrivateIpAddress())
				.managerPublicIpAddress(managerHostAddress.getPublicIpAddress()).managerPrivateIpAddress(managerHostAddress.getPrivateIpAddress())
				.decision(decision).count(0).build());
		if (!Objects.equals(hostEvent.getDecision().getId(), decision.getId())) {
			hostEvent.setDecision(decision);
			hostEvent.setCount(1);
		}
		else {
			hostEvent.setCount(hostEvent.getCount() + 1);
		}
		return addHostEvent(hostEvent);
	}

	public void reset() {
		log.info("Clearing all host events");
		decisionsService.getDecisions().forEach(Decision::removeHostEvents);
		hostEvents.deleteAll();
	}

	public void deleteEvents(HostAddress hostAddress) {
		List<HostEvent> events = getHostEventsByHostAddress(hostAddress);
		log.info("Deleting events {} from host {}", events, hostAddress.toSimpleString());
		hostEvents.deleteAll(events);
	}

	public void reset(HostAddress hostAddress) {
		Optional<HostEvent> hostEvent = getHostEventsByHostAddress(hostAddress).stream().findFirst();
		hostEvent.ifPresent(event -> {
			event.setCount(0);
			hostEvents.save(event);
		});
	}
}
