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

package pt.unl.fct.miei.usmanagement.manager.management.services.discovery.registration;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.unl.fct.miei.usmanagement.manager.containers.Container;
import pt.unl.fct.miei.usmanagement.manager.exceptions.BadRequestException;
import pt.unl.fct.miei.usmanagement.manager.hosts.HostAddress;
import pt.unl.fct.miei.usmanagement.manager.regions.RegionEnum;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/registration-server")
public class RegistrationServerController {

	private final RegistrationServerService registrationServerService;

	public RegistrationServerController(RegistrationServerService registrationServerService) {
		this.registrationServerService = registrationServerService;
	}

	@PostMapping
	public List<Container> launchRegistration(@RequestBody LaunchRegistrationServer launchRegistrationServer) {
		HostAddress hostAddress = launchRegistrationServer.getHostAddress();
		List<String> regions = launchRegistrationServer.getRegions();
		if (hostAddress != null) {
			return List.of(registrationServerService.launchRegistrationServer(hostAddress));
		}
		else if (regions != null) {
			List<RegionEnum> regionsList = Arrays.stream(regions.toArray(new String[0])).map(RegionEnum::getRegion).collect(Collectors.toList());
			return registrationServerService.launchRegistrationServers(regionsList);
		}
		else {
			throw new BadRequestException("Expected host address or regions to start registration server");
		}
	}

}
