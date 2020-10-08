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

package pt.unl.fct.miei.usmanagement.manager.database.monitoring;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pt.unl.fct.miei.usmanagement.manager.database.hosts.cloud.CloudHostEntity;
import pt.unl.fct.miei.usmanagement.manager.database.hosts.edge.EdgeHostEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface HostSimulatedMetricsRepository extends JpaRepository<HostSimulatedMetricEntity, Long> {

	Optional<HostSimulatedMetricEntity> findByNameIgnoreCase(@Param("name") String name);

	@Query("select m "
		+ "from HostSimulatedMetricEntity m join m.cloudHosts c join m.edgeHosts e "
		+ "where (c.publicIpAddress = :publicIpAddress and c.privateIpAddress = :privateIpAddress) or "
		+ "(e.publicIpAddress = :publicIpAddress and e.privateIpAddress = :privateIpAddress)")
	List<HostSimulatedMetricEntity> findByHostAddress(@Param("publicIpAddress") String publicIpAddress,
													  @Param("privateIpAddress") String privateIpAddress);

	@Query("select m "
		+ "from HostSimulatedMetricEntity m join m.cloudHosts c join m.edgeHosts e "
		+ "where ((c.publicIpAddress = :publicIpAddress and c.privateIpAddress = :privateIpAddress) or "
		+ "(e.publicIpAddress = :publicIpAddress and e.privateIpAddress = :privateIpAddress)) "
		+ "and lower(m.field.name) = lower(:field)")
	Optional<HostSimulatedMetricEntity> findByHostAndField(@Param("publicIpAddress") String publicIpAddress,
														   @Param("privateIpAddress") String privateIpAddress,
														   @Param("field") String field);

	@Query("select m "
		+ "from HostSimulatedMetricEntity m join m.cloudHosts h "
		+ "where h.publicIpAddress = :hostname")
	List<HostSimulatedMetricEntity> findByCloudHost(@Param("hostname") String hostname);

	@Query("select m "
		+ "from HostSimulatedMetricEntity m join m.cloudHosts h "
		+ "where h.publicIpAddress = :hostname and lower(m.field.name) = lower(:field)")
	Optional<HostSimulatedMetricEntity> findByCloudHostAndField(@Param("field") String field,
																@Param("hostname") String hostname);

	@Query("select m "
		+ "from HostSimulatedMetricEntity m join m.edgeHosts h "
		+ "where h.publicDnsName = :hostname or h.publicIpAddress = :hostname")
	List<HostSimulatedMetricEntity> findByEdgeHost(@Param("hostname") String hostname);

	@Query("select m "
		+ "from HostSimulatedMetricEntity m join m.edgeHosts h "
		+ "where h.publicDnsName = :publicDnsName and lower(m.field.name) = lower(:field)")
	Optional<HostSimulatedMetricEntity> findByEdgeHostAndField(@Param("field") String field,
															   @Param("publicDnsName") String publicDnsName);

	@Query("select m "
		+ "from HostSimulatedMetricEntity m "
		+ "where m.generic = true and lower(m.field.name) = lower(:field)")
	Optional<HostSimulatedMetricEntity> findGenericByField(@Param("field") String field);

	@Query("select m "
		+ "from HostSimulatedMetricEntity m "
		+ "where m.generic = true")
	List<HostSimulatedMetricEntity> findGenericHostSimulatedMetrics();

	@Query("select m "
		+ "from HostSimulatedMetricEntity m "
		+ "where m.generic = true and lower(m.name) = lower(:simulatedMetricName)")
	Optional<HostSimulatedMetricEntity> findGenericHostSimulatedMetric(@Param("simulatedMetricName")
																		   String simulatedMetricName);

	@Query("select case when count(m) > 0 then true else false end "
		+ "from HostSimulatedMetricEntity m "
		+ "where lower(m.name) = lower(:simulatedMetricName)")
	boolean hasHostSimulatedMetric(@Param("simulatedMetricName") String simulatedMetricName);

	@Query("select h "
		+ "from HostSimulatedMetricEntity m join m.cloudHosts h "
		+ "where lower(m.name) = lower(:simulatedMetricName)")
	List<CloudHostEntity> getCloudHosts(@Param("simulatedMetricName") String simulatedMetricName);

	@Query("select h "
		+ "from HostSimulatedMetricEntity m join m.cloudHosts h "
		+ "where lower(m.name) = lower(:simulatedMetricName) and h.instanceId = :instanceId")
	Optional<CloudHostEntity> getCloudHost(@Param("simulatedMetricName") String simulatedMetricName,
										   @Param("instanceId") String instanceId);

	@Query("select h "
		+ "from HostSimulatedMetricEntity m join m.edgeHosts h "
		+ "where lower(m.name) = lower(:simulatedMetricName)")
	List<EdgeHostEntity> getEdgeHosts(@Param("simulatedMetricName") String simulatedMetricName);

	@Query("select h "
		+ "from HostSimulatedMetricEntity m join m.edgeHosts h "
		+ "where lower(m.name) = lower(:simulatedMetricName) "
		+ "and (h.publicDnsName = :hostname or h.publicIpAddress = :hostname)")
	Optional<EdgeHostEntity> getEdgeHost(@Param("simulatedMetricName") String simulatedMetricName,
										 @Param("hostname") String hostname);

}
