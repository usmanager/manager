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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import pt.unl.fct.miei.usmanagement.manager.apps.App;
import pt.unl.fct.miei.usmanagement.manager.exceptions.EntityNotFoundException;
import pt.unl.fct.miei.usmanagement.manager.hosts.HostAddress;
import pt.unl.fct.miei.usmanagement.manager.operators.OperatorEnum;
import pt.unl.fct.miei.usmanagement.manager.rulesystem.condition.Condition;
import pt.unl.fct.miei.usmanagement.manager.rulesystem.rules.AppRule;
import pt.unl.fct.miei.usmanagement.manager.rulesystem.rules.ContainerRule;
import pt.unl.fct.miei.usmanagement.manager.rulesystem.rules.RuleConditionKey;
import pt.unl.fct.miei.usmanagement.manager.rulesystem.rules.RuleDecisionEnum;
import pt.unl.fct.miei.usmanagement.manager.rulesystem.rules.ServiceRule;
import pt.unl.fct.miei.usmanagement.manager.rulesystem.rules.ServiceRuleCondition;
import pt.unl.fct.miei.usmanagement.manager.rulesystem.rules.ServiceRules;
import pt.unl.fct.miei.usmanagement.manager.services.Service;
import pt.unl.fct.miei.usmanagement.manager.services.communication.kafka.KafkaService;
import pt.unl.fct.miei.usmanagement.manager.services.containers.ContainersService;
import pt.unl.fct.miei.usmanagement.manager.services.monitoring.events.ContainerEvent;
import pt.unl.fct.miei.usmanagement.manager.services.rulesystem.RuleConditionsService;
import pt.unl.fct.miei.usmanagement.manager.services.rulesystem.condition.ConditionsService;
import pt.unl.fct.miei.usmanagement.manager.services.rulesystem.decision.ServiceDecisionResult;
import pt.unl.fct.miei.usmanagement.manager.services.services.ServicesService;
import pt.unl.fct.miei.usmanagement.manager.util.EntityUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Slf4j
@org.springframework.stereotype.Service
public class ServiceRulesService {

	private final ConditionsService conditionsService;
	private final RuleConditionsService ruleConditionsService;
	private final DroolsService droolsService;
	private final ServicesService servicesService;
	private final ContainersService containersService;
	private final AppRulesService appRulesService;
	private final ContainerRulesService containerRulesService;
	private final KafkaService kafkaService;

	private final ServiceRules rules;

	private final String serviceRuleTemplateFile;
	private final AtomicLong lastUpdateServiceRules;

	public ServiceRulesService(ConditionsService conditionsService, RuleConditionsService ruleConditionsService, DroolsService droolsService,
							   @Lazy ServicesService servicesService, @Lazy ContainersService containersService,
							   @Lazy AppRulesService appRulesService,  @Lazy ContainerRulesService containerRulesService,
							   KafkaService kafkaService, ServiceRules rules, RulesProperties rulesProperties) {
		this.conditionsService = conditionsService;
		this.ruleConditionsService = ruleConditionsService;
		this.droolsService = droolsService;
		this.servicesService = servicesService;
		this.containersService = containersService;
		this.appRulesService = appRulesService;
		this.containerRulesService = containerRulesService;
		this.kafkaService = kafkaService;
		this.rules = rules;
		this.serviceRuleTemplateFile = rulesProperties.getServiceRuleTemplateFile();
		this.lastUpdateServiceRules = new AtomicLong(0);
	}

	public void setLastUpdateServiceRules() {
		long currentTime = System.currentTimeMillis();
		lastUpdateServiceRules.getAndSet(currentTime);
	}

	@Transactional(readOnly = true)
	public List<ServiceRule> getRules() {
		return rules.findAll();
	}

	public ServiceRule getRule(Long id) {
		return rules.findById(id).orElseThrow(() ->
			new EntityNotFoundException(ServiceRule.class, "id", id.toString()));
	}

	public ServiceRule getRule(String name) {
		return rules.findByNameIgnoreCase(name).orElseThrow(() ->
			new EntityNotFoundException(ServiceRule.class, "name", name));
	}

	public ServiceRule addRule(ServiceRule rule) {
		checkRuleDoesntExist(rule);
		rule = saveRule(rule);
		/*ServiceRule kafkaRule = rule;
		kafkaRule.setNew(true);
		kafkaService.sendServiceRule(kafkaRule);*/
		kafkaService.sendServiceRule(rule);
		return rule;
	}

	public ServiceRule updateRule(String ruleName, ServiceRule newRule) {
		log.info("Updating rule {} with {}", ruleName, ToStringBuilder.reflectionToString(newRule));
		ServiceRule rule = getRule(ruleName);
		EntityUtils.copyValidProperties(newRule, rule);
		rule = saveRule(rule);
		kafkaService.sendServiceRule(rule);
		return rule;
	}

	public ServiceRule saveRule(ServiceRule serviceRule) {
		log.info("Saving serviceRule {}", ToStringBuilder.reflectionToString(serviceRule));
		serviceRule = rules.save(serviceRule);
		setLastUpdateServiceRules();
		return serviceRule;
	}

	public ServiceRule addIfNotPresent(ServiceRule serviceRule) {
		Optional<ServiceRule> serviceRuleOptional = rules.findById(serviceRule.getId());
		return serviceRuleOptional.orElseGet(() -> {
			serviceRule.clearAssociations();
			return saveRule(serviceRule);
		});
	}

	public ServiceRule addOrUpdateRule(ServiceRule serviceRule) {
		if (serviceRule.getId() != null) {
			Optional<ServiceRule> serviceRuleOptional = rules.findById(serviceRule.getId());
			if (serviceRuleOptional.isPresent()) {
				ServiceRule rule = serviceRuleOptional.get();
				Set<ServiceRuleCondition> conditions = serviceRule.getConditions();
				if (conditions != null) {
					rule.getConditions().retainAll(conditions);
					rule.getConditions().addAll(conditions);
				}
				Set<Service> services = serviceRule.getServices();
				if (services != null) {
					rule.getServices().retainAll(serviceRule.getServices());
					rule.getServices().addAll(serviceRule.getServices());
				}
				EntityUtils.copyValidProperties(serviceRule, rule);
				return saveRule(rule);
			}
		}
		return saveRule(serviceRule);
	}

	public void deleteRule(Long id) {
		log.info("Deleting rule {}", id);
		ServiceRule rule = getRule(id);
		deleteRule(rule, false);
	}

	public void deleteRule(String ruleName) {
		log.info("Deleting rule {}", ruleName);
		ServiceRule rule = getRule(ruleName);
		deleteRule(rule, true);
	}

	public void deleteRule(ServiceRule rule, boolean kafka) {
		rule.removeAssociations();
		rules.delete(rule);
		setLastUpdateServiceRules();
		if (kafka) {
			kafkaService.sendDeleteServiceRule(rule);
		}
	}

	public List<ServiceRule> getServiceRules(String serviceName) {
		return rules.findByServiceName(serviceName);
	}

	public List<ServiceRule> getGenericServiceRules() {
		return rules.findGenericServiceRules();
	}

	public Condition getCondition(String ruleName, String conditionName) {
		checkRuleExists(ruleName);
		return rules.getCondition(ruleName, conditionName).orElseThrow(() ->
			new EntityNotFoundException(Condition.class, "conditionName", conditionName));
	}

	public List<Condition> getConditions(String ruleName) {
		checkRuleExists(ruleName);
		return rules.getConditions(ruleName);
	}

	public void addCondition(String ruleName, String conditionName) {
		log.info("Adding condition {} to rule {}", conditionName, ruleName);
		Condition condition = conditionsService.getCondition(conditionName);
		ServiceRule rule = getRule(ruleName);
		ServiceRuleCondition serviceRuleCondition = ServiceRuleCondition.builder()
			.id(new RuleConditionKey(rule.getId(), condition.getId()))
			.rule(rule).condition(condition).build();
		ruleConditionsService.saveServiceRuleCondition(serviceRuleCondition);
		rule = rule.toBuilder().condition(serviceRuleCondition).build();
		kafkaService.sendServiceRule(rule);
	}

	public void addConditions(String ruleName, List<String> conditions) {
		conditions.forEach(condition -> addCondition(ruleName, condition));
	}

	public void removeCondition(String ruleName, String conditionName) {
		removeConditions(ruleName, List.of(conditionName));
	}

	public void removeConditions(String ruleName, List<String> conditionNames) {
		log.info("Removing conditions {}", conditionNames);
		ServiceRule rule = getRule(ruleName);
		rule.getConditions().removeIf(condition -> conditionNames.contains(condition.getCondition().getName()));
		rule = saveRule(rule);
		kafkaService.sendServiceRule(rule);
	}

	public Service getService(String ruleName, String serviceName) {
		checkRuleExists(ruleName);
		return rules.getService(ruleName, serviceName).orElseThrow(() ->
			new EntityNotFoundException(Service.class, "serviceName", serviceName));
	}

	public List<Service> getServices(String ruleName) {
		checkRuleExists(ruleName);
		return rules.getServices(ruleName);
	}

	public void addService(String ruleName, String serviceName) {
		addServices(ruleName, List.of(serviceName));
	}

	public void addServices(String ruleName, List<String> serviceNames) {
		log.info("Adding services {} to rule {}", serviceNames, ruleName);
		ServiceRule rule = getRule(ruleName);
		serviceNames.forEach(serviceName -> {
			Service service = servicesService.getService(serviceName);
			service.addRule(rule);
		});
		ServiceRule serviceRule = saveRule(rule);
		kafkaService.sendServiceRule(serviceRule);
	}

	public void removeService(String ruleName, String serviceName) {
		removeServices(ruleName, List.of(serviceName));
	}

	public void removeServices(String ruleName, List<String> serviceNames) {
		log.info("Removing services {} from rule {}", serviceNames, ruleName);
		ServiceRule rule = getRule(ruleName);
		serviceNames.forEach(serviceName -> servicesService.getService(serviceName).removeRule(rule));
		ServiceRule serviceRule = saveRule(rule);
		kafkaService.sendServiceRule(serviceRule);
	}

	private void checkRuleExists(String ruleName) {
		if (!rules.hasRule(ruleName)) {
			throw new EntityNotFoundException(ServiceRule.class, "ruleName", ruleName);
		}
	}

	private void checkRuleDoesntExist(ServiceRule serviceRule) {
		String name = serviceRule.getName();
		if (rules.hasRule(name)) {
			throw new DataIntegrityViolationException("Service rule '" + name + "' already exists");
		}
	}

	public ServiceDecisionResult processServiceEvent(HostAddress hostAddress, ContainerEvent containerEvent) {
		String serviceName = containerEvent.getServiceName();
		String containerId = containerEvent.getContainerId();
		if (droolsService.shouldCreateNewServiceRuleSession(serviceName, lastUpdateServiceRules.get())) {
			List<Rule> rules = generateServiceRules(serviceName, containerId);
			Map<Long, String> drools = droolsService.executeDroolsRules(containerEvent, rules, serviceRuleTemplateFile);
			droolsService.createNewServiceRuleSession(serviceName, drools);
		}
		return droolsService.evaluate(hostAddress, containerEvent);
	}

	private List<Rule> generateServiceRules(String serviceName, String containerId) {
		List<ServiceRule> genericServiceRules = getGenericServiceRules();
		List<AppRule> appRules = new LinkedList<>();
		for (App app : servicesService.getAppsAndAppRules(serviceName)) {
			appRules.addAll(app.getAppRules());
		}
		List<ServiceRule> serviceRules = getServiceRules(serviceName);
		List<ContainerRule> containerRules = new ArrayList<>(containersService.getRules(containerId));
		int count = genericServiceRules.size() + appRules.size() + serviceRules.size() + containerRules.size();
		List<Rule> rules = new ArrayList<>(count);
		log.info("Generating service rules... (count: {})", count);
		genericServiceRules.forEach(genericServiceRule -> rules.add(generateServiceRule(genericServiceRule)));
		appRules.forEach(appRule -> rules.add(generateServiceRule(appRule)));
		serviceRules.forEach(serviceRule -> rules.add(generateServiceRule(serviceRule)));
		containerRules.forEach(containerRule -> rules.add(generateServiceRule(containerRule)));
		return rules;
	}

	private Rule generateServiceRule(AppRule appRule) {
		return generateRule(appRule.getId(), appRule.getDecision().getRuleDecision(),
			appRule.getPriority(), appRulesService.getConditions(appRule.getName()));
	}

	private Rule generateServiceRule(ServiceRule serviceRule) {
		return generateRule(serviceRule.getId(), serviceRule.getDecision().getRuleDecision(),
			serviceRule.getPriority(), getConditions(serviceRule.getName()));
	}

	private Rule generateServiceRule(ContainerRule containerRule) {
		return generateRule(containerRule.getId(), containerRule.getDecision().getRuleDecision(),
			containerRule.getPriority(), containerRulesService.getConditions(containerRule.getName()));
	}

	private Rule generateRule(Long id, RuleDecisionEnum decision, int priority, List<Condition> entityConditions) {
		List<pt.unl.fct.miei.usmanagement.manager.services.rulesystem.condition.Condition> conditions = entityConditions
			.stream().map(condition -> {
				String fieldName = String.format("%s-%s", condition.getField().getName(), condition.getValueMode().getName().toLowerCase());
				double value = condition.getValue();
				OperatorEnum operator = condition.getOperator().getOperator();
				return new pt.unl.fct.miei.usmanagement.manager.services.rulesystem.condition.Condition(fieldName, value, operator);
			}).collect(Collectors.toList());
		return new Rule(id, conditions, decision, priority);
	}

}