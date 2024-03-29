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

package pt.unl.fct.miei.usmanagement.manager.rulesystem.decision;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pt.unl.fct.miei.usmanagement.manager.componenttypes.ComponentTypeEnum;
import pt.unl.fct.miei.usmanagement.manager.rulesystem.rules.RuleDecisionEnum;

import java.util.List;
import java.util.Optional;

public interface Decisions extends JpaRepository<Decision, Long> {

	Optional<Decision> findByRuleDecision(@Param("decision") RuleDecisionEnum decision);

	List<Decision> findByComponentTypeType(@Param("componentType") ComponentTypeEnum componentType);

	Optional<Decision> findByRuleDecisionAndComponentTypeType(@Param("decision") RuleDecisionEnum decision,
															  @Param("componentType") ComponentTypeEnum componentType);

	@Query("select case when count(d) > 0 then true else false end "
		+ "from Decision d "
		+ "where lower(d.id) = lower(:id)")
	boolean hasDecision(Long id);
}
