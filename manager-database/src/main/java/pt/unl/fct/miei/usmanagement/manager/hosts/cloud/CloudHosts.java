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

package pt.unl.fct.miei.usmanagement.manager.hosts.cloud;

import com.amazonaws.services.ec2.model.InstanceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pt.unl.fct.miei.usmanagement.manager.metrics.simulated.HostSimulatedMetric;
import pt.unl.fct.miei.usmanagement.manager.regions.RegionEnum;
import pt.unl.fct.miei.usmanagement.manager.rulesystem.rules.HostRule;

import java.util.List;
import java.util.Optional;

public interface CloudHosts extends JpaRepository<CloudHost, Long> {

	@Query("select h "
		+ "from CloudHost h left join fetch h.managedByWorker "
		+ "where h.id = :id")
	Optional<CloudHost> getCloudHostWithWorker(@Param("id") Long id);

	Optional<CloudHost> findByInstanceId(@Param("instanceId") String instanceId);

	Optional<CloudHost> findByPublicIpAddress(@Param("publicIpAddress") String publicIpAddress);

	Optional<CloudHost> findByInstanceIdOrPublicDnsName(@Param("instanceId") String instanceId,
														@Param("publicDnsName") String publicDnsName);

	Optional<CloudHost> findByInstanceIdOrPublicIpAddress(@Param("instanceId") String instanceId,
														  @Param("publicIpAddress") String publicIpAddress);

	@Query("select h "
		+ "from CloudHost h "
		+ "where h.publicIpAddress = :publicIpAddress and h.privateIpAddress = :privateIpAddress")
	Optional<CloudHost> findByAddress(@Param("publicIpAddress") String publicIpAddress,
									  @Param("privateIpAddress") String privateIpAddress);

	@Query("select r "
		+ "from CloudHost h left join h.hostRules r "
		+ "where r.generic = false and (h.publicIpAddress = :hostname or h.instanceId = :hostname)")
	List<HostRule> getRules(@Param("hostname") String hostname);

	@Query("select r "
		+ "from CloudHost h left join h.hostRules r "
		+ "where r.generic = false and (h.publicIpAddress = :hostname or h.instanceId = :hostname) "
		+ "and r.name = :ruleName")
	Optional<HostRule> getRule(@Param("hostname") String hostname, @Param("ruleName") String ruleName);

	@Query("select m "
		+ "from CloudHost h left join h.simulatedHostMetrics m "
		+ "where h.publicIpAddress = :hostname or h.instanceId = :hostname")
	List<HostSimulatedMetric> getSimulatedMetrics(@Param("hostname") String hostname);

	@Query("select m "
		+ "from CloudHost h left join h.simulatedHostMetrics m "
		+ "where (h.publicIpAddress = :hostname or h.instanceId = :hostname) and m.name = :simulatedMetricName")
	Optional<HostSimulatedMetric> getSimulatedMetric(@Param("hostname") String hostname,
													 @Param("simulatedMetricName") String simulatedMetricName);

	@Query("select case when count(h) > 0 then true else false end "
		+ "from CloudHost h "
		+ "where h.publicIpAddress = :hostname or h.instanceId = :hostname")
	boolean hasCloudHost(@Param("hostname") String hostname);

	@Query("select h "
		+ "from CloudHost h left join fetch h.hostRules left join fetch h.simulatedHostMetrics")
	List<CloudHost> getCloudHostsAndRelations();

	@Query("select h "
		+ "from CloudHost h "
		+ "where h.instanceType = :type")
	List<CloudHost> findByInstanceType(String type);
}
