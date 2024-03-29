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

package pt.unl.fct.miei.usmanagement.manager.services.rulesystem.rules;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import pt.unl.fct.miei.usmanagement.manager.rulesystem.rules.RuleDecisionEnum;
import pt.unl.fct.miei.usmanagement.manager.services.monitoring.events.EventType;
import pt.unl.fct.miei.usmanagement.manager.services.rulesystem.condition.Condition;

import java.util.List;

@Slf4j
@Data
public final class Rule {

	private final long id;
	private final List<Condition> conditions;
	private final RuleDecisionEnum decision;
	private final int priority;
	private final EventType eventType;

	public Rule(long id, List<Condition> conditions, RuleDecisionEnum decision, int priority) {
		this.id = id;
		this.conditions = conditions;
		this.decision = decision;
		this.priority = priority;
		this.eventType = null;
	}

	@Override
	public String toString() {
		StringBuilder statementBuilder = new StringBuilder();

		for (Condition condition : getConditions()) {
			String operator = null;

			switch (condition.getOperator()) {
				case EQUAL_TO:
					operator = "==";
					break;
				case NOT_EQUAL_TO:
					operator = "!=";
					break;
				case GREATER_THAN:
					operator = ">";
					break;
				case LESS_THAN:
					operator = "<";
					break;
				case GREATER_THAN_OR_EQUAL_TO:
					operator = ">=";
					break;
				case LESS_THAN_OR_EQUAL_TO:
					operator = "<=";
					break;
			}
			statementBuilder.append("fields[\"");
			statementBuilder.append(condition.getFieldName()).append("\"] ").append(operator).append(" ");
			statementBuilder.append(condition.getValue());
			statementBuilder.append(" && ");
		}

		String statement = statementBuilder.toString();

		// remove trailing &&
		return statement.length() > 0 ? statement.substring(0, statement.length() - 4) : statement;
	}

}