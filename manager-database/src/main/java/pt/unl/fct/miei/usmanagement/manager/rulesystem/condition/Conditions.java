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

package pt.unl.fct.miei.usmanagement.manager.rulesystem.condition;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pt.unl.fct.miei.usmanagement.manager.rulesystem.rules.AppRuleCondition;
import pt.unl.fct.miei.usmanagement.manager.rulesystem.rules.ContainerRuleCondition;
import pt.unl.fct.miei.usmanagement.manager.rulesystem.rules.HostRuleCondition;
import pt.unl.fct.miei.usmanagement.manager.rulesystem.rules.ServiceRuleCondition;

import java.util.List;
import java.util.Optional;

public interface Conditions extends JpaRepository<Condition, Long> {

	@Query("select case when count(c) > 0 then true else false end "
		+ "from Condition c "
		+ "where lower(c.name) = lower(:name)")
	boolean hasCondition(@Param("name") String name);

	Optional<Condition> findByNameIgnoreCase(@Param("name") String name);

	@Query("select c.hostConditions "
		+ "from Condition c "
		+ "where lower(c.name) = lower(:name)")
	List<HostRuleCondition> getHostRuleConditions(String name);

	@Query("select c.appConditions "
		+ "from Condition c "
		+ "where lower(c.name) = lower(:name)")
	List<AppRuleCondition> getAppRuleConditions(String name);

	@Query("select c.serviceConditions "
		+ "from Condition c "
		+ "where lower(c.name) = lower(:name)")
	List<ServiceRuleCondition> getServiceRuleConditions(String name);

	@Query("select c.containerConditions "
		+ "from Condition c "
		+ "where lower(c.name) = lower(:name)")
	List<ContainerRuleCondition> getContainerRuleConditions(String name);

}
