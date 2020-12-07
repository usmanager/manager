package pt.unl.fct.miei.usmanagement.manager.management.communication.kafka;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import pt.unl.fct.miei.usmanagement.manager.rulesystem.rules.AppRuleCondition;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
public class AppRuleConditionMessage {

	private AppRuleMessage appRuleMessage;
	private ConditionMessage conditionMessage;

	public AppRuleConditionMessage(AppRuleCondition appRuleCondition) {
		this.appRuleMessage = new AppRuleMessage(appRuleCondition.getAppRule());
		this.conditionMessage = new ConditionMessage(appRuleCondition.getAppCondition());
	}

	public AppRuleCondition get() {
		return AppRuleCondition.builder()
			.appRule(appRuleMessage.get())
			.appCondition(conditionMessage.get())
			.build();
	}
}
