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

package pt.unl.fct.miei.usmanagement.manager.services.docker.containers;

import lombok.Data;
import pt.unl.fct.miei.usmanagement.manager.containers.ContainerPortMapping;
import pt.unl.fct.miei.usmanagement.manager.containers.ContainerTypeEnum;
import pt.unl.fct.miei.usmanagement.manager.hosts.Coordinates;
import pt.unl.fct.miei.usmanagement.manager.hosts.HostAddress;
import pt.unl.fct.miei.usmanagement.manager.regions.RegionEnum;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Data
public final class DockerContainer {

	private final String id;
	private final ContainerTypeEnum type;
	private final long created;
	private final String name;
	private final String image;
	private final String command;
	private final String network;
	private final String state;
	private final String status;
	private final String publicIpAddress;
	private final String privateIpAddress;
	private final Set<String> mounts;
	private final Coordinates coordinates;
	private final RegionEnum region;
	private final Set<ContainerPortMapping> ports;
	private final Map<String, String> labels;

	public HostAddress getHostAddress() {
		return new HostAddress(publicIpAddress, privateIpAddress, coordinates);
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
		if (!(o instanceof DockerContainer)) {
			return false;
		}
		DockerContainer other = (DockerContainer) o;
		return id != null && id.equals(other.getId());
	}

}
