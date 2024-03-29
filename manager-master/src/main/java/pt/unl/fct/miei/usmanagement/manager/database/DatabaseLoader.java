/* MIT License
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


package pt.unl.fct.miei.usmanagement.manager.database;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pt.unl.fct.miei.usmanagement.manager.apps.App;
import pt.unl.fct.miei.usmanagement.manager.apps.AppService;
import pt.unl.fct.miei.usmanagement.manager.apps.AppServiceKey;
import pt.unl.fct.miei.usmanagement.manager.apps.AppServices;
import pt.unl.fct.miei.usmanagement.manager.componenttypes.ComponentType;
import pt.unl.fct.miei.usmanagement.manager.componenttypes.ComponentTypeEnum;
import pt.unl.fct.miei.usmanagement.manager.exceptions.EntityNotFoundException;
import pt.unl.fct.miei.usmanagement.manager.fields.Field;
import pt.unl.fct.miei.usmanagement.manager.hosts.Coordinates;
import pt.unl.fct.miei.usmanagement.manager.hosts.HostAddress;
import pt.unl.fct.miei.usmanagement.manager.hosts.cloud.CloudHost;
import pt.unl.fct.miei.usmanagement.manager.hosts.edge.EdgeHost;
import pt.unl.fct.miei.usmanagement.manager.management.monitoring.HostsMonitoringService;
import pt.unl.fct.miei.usmanagement.manager.management.monitoring.ServicesMonitoringService;
import pt.unl.fct.miei.usmanagement.manager.metrics.PrometheusQueryEnum;
import pt.unl.fct.miei.usmanagement.manager.operators.Operator;
import pt.unl.fct.miei.usmanagement.manager.operators.OperatorEnum;
import pt.unl.fct.miei.usmanagement.manager.regions.RegionEnum;
import pt.unl.fct.miei.usmanagement.manager.rulesystem.condition.Condition;
import pt.unl.fct.miei.usmanagement.manager.rulesystem.decision.Decision;
import pt.unl.fct.miei.usmanagement.manager.rulesystem.rules.HostRule;
import pt.unl.fct.miei.usmanagement.manager.rulesystem.rules.HostRuleCondition;
import pt.unl.fct.miei.usmanagement.manager.rulesystem.rules.RuleConditionKey;
import pt.unl.fct.miei.usmanagement.manager.rulesystem.rules.RuleDecisionEnum;
import pt.unl.fct.miei.usmanagement.manager.rulesystem.rules.ServiceRule;
import pt.unl.fct.miei.usmanagement.manager.rulesystem.rules.ServiceRuleCondition;
import pt.unl.fct.miei.usmanagement.manager.rulesystem.rules.ServiceRuleConditions;
import pt.unl.fct.miei.usmanagement.manager.services.Service;
import pt.unl.fct.miei.usmanagement.manager.services.ServiceConstants;
import pt.unl.fct.miei.usmanagement.manager.services.ServiceTypeEnum;
import pt.unl.fct.miei.usmanagement.manager.services.apps.AppsService;
import pt.unl.fct.miei.usmanagement.manager.services.communication.kafka.KafkaService;
import pt.unl.fct.miei.usmanagement.manager.services.communication.zookeeper.ZookeeperService;
import pt.unl.fct.miei.usmanagement.manager.services.componenttypes.ComponentTypesService;
import pt.unl.fct.miei.usmanagement.manager.services.configurations.ConfigurationsService;
import pt.unl.fct.miei.usmanagement.manager.services.containers.ContainersService;
import pt.unl.fct.miei.usmanagement.manager.services.docker.DockerProperties;
import pt.unl.fct.miei.usmanagement.manager.services.docker.nodes.NodesService;
import pt.unl.fct.miei.usmanagement.manager.services.eips.ElasticIpsService;
import pt.unl.fct.miei.usmanagement.manager.services.fields.FieldsService;
import pt.unl.fct.miei.usmanagement.manager.services.heartbeats.HeartbeatService;
import pt.unl.fct.miei.usmanagement.manager.services.hosts.edge.EdgeHostsService;
import pt.unl.fct.miei.usmanagement.manager.services.loadbalancer.nginx.LoadBalancerService;
import pt.unl.fct.miei.usmanagement.manager.services.monitoring.events.HostsEventsService;
import pt.unl.fct.miei.usmanagement.manager.services.monitoring.events.ServicesEventsService;
import pt.unl.fct.miei.usmanagement.manager.services.operators.OperatorsService;
import pt.unl.fct.miei.usmanagement.manager.services.rulesystem.RuleConditionsService;
import pt.unl.fct.miei.usmanagement.manager.services.rulesystem.condition.ConditionsService;
import pt.unl.fct.miei.usmanagement.manager.services.rulesystem.decision.DecisionsService;
import pt.unl.fct.miei.usmanagement.manager.services.rulesystem.rules.HostRulesService;
import pt.unl.fct.miei.usmanagement.manager.services.rulesystem.rules.ServiceRulesService;
import pt.unl.fct.miei.usmanagement.manager.services.services.ServiceDependenciesService;
import pt.unl.fct.miei.usmanagement.manager.services.services.ServicesService;
import pt.unl.fct.miei.usmanagement.manager.services.services.discovery.registration.RegistrationServerService;
import pt.unl.fct.miei.usmanagement.manager.services.valuemodes.ValueModesService;
import pt.unl.fct.miei.usmanagement.manager.services.workermanagers.WorkerManagersService;
import pt.unl.fct.miei.usmanagement.manager.sync.SyncService;
import pt.unl.fct.miei.usmanagement.manager.services.users.User;
import pt.unl.fct.miei.usmanagement.manager.services.users.UserRoleEnum;
import pt.unl.fct.miei.usmanagement.manager.services.users.UsersService;
import pt.unl.fct.miei.usmanagement.manager.valuemodes.ValueMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DatabaseLoader {

	@Bean
	CommandLineRunner initDatabase(UsersService usersService, ServicesService servicesService, AppsService appsService,
								   AppServices appServices, ServiceDependenciesService serviceDependenciesService,
		/*RegionsService regionsService,*/
								   EdgeHostsService edgeHostsService, ComponentTypesService componentTypesService,
								   OperatorsService operatorsService, DecisionsService decisionsService,
								   FieldsService fieldsService, ValueModesService valueModesService,
								   ConditionsService conditionsService, HostRulesService hostRulesService,
								   RuleConditionsService ruleConditionsService,
								   ServiceRulesService serviceRulesService,
								   ServiceRuleConditions serviceRuleConditions, DockerProperties dockerProperties,
								   HostsEventsService hostsEventsService, ServicesEventsService servicesEventsService,
								   HostsMonitoringService hostsMonitoringService, ServicesMonitoringService servicesMonitoringService,
								   NodesService nodesService, ElasticIpsService elasticIpsService, SyncService syncService,
								   ContainersService containersService, WorkerManagersService workerManagersService,
								   ConfigurationsService configurationsService, LoadBalancerService nginxLoadBalancerService,
								   RegistrationServerService registrationServerService, ZookeeperService zookeeperService,
								   KafkaService kafkaService, HeartbeatService heartbeatService) {
		return args -> {

			Map<String, User> users = loadUsers(usersService);

			String dockerHubUsername = dockerProperties.getHub().getUsername();
			Map<String, Service> services = new HashMap<>(loadSystemComponents(dockerHubUsername, servicesService, serviceDependenciesService, dockerProperties));
			services.putAll(loadSockShop(dockerHubUsername, appsService, servicesService, serviceDependenciesService, appServices));
			//services.putAll(loadMixal(dockerHubUsername, appsService, servicesService, serviceDependenciesService, appServices));
			services.putAll(loadOnlineBoutique(dockerHubUsername, appsService, servicesService, serviceDependenciesService, appServices));
			services.putAll(loadHotelReservation(dockerHubUsername, appsService, servicesService, serviceDependenciesService, appServices));
			services.putAll(loadMediaMicroservices(dockerHubUsername, appsService, servicesService, serviceDependenciesService, appServices));
			services.putAll(loadSocialNetwork(dockerHubUsername, appsService, servicesService, serviceDependenciesService, appServices));
			services.putAll(loadTestingSuite(dockerHubUsername, appsService, servicesService, appServices));

			/*Map<RegionEnum, Region> regions = loadRegions(regionsService);*/

			Map<ComponentTypeEnum, ComponentType> componentTypes = loadComponentTypes(componentTypesService);

			Map<OperatorEnum, Operator> operators = loadOperators(operatorsService);

			Map<RuleDecisionEnum, Decision> decisions = loadDecisions(decisionsService, componentTypes);

			Map<String, Field> fields = loadFields(fieldsService);

			Map<String, ValueMode> valueModes = loadValueModes(valueModesService);

			Map<String, Condition> conditions = loadConditions(conditionsService, valueModes, fields, operators);

			Map<String, HostRule> hostRules = loadRules(hostRulesService, ruleConditionsService, decisions, conditions);

			Map<String, ServiceRule> serviceRules = loadServiceRules(serviceRulesService, serviceRuleConditions, decisions, conditions);

			List<EdgeHost> edgeHosts = loadEdgeHosts(edgeHostsService);

			List<CloudHost> cloudHosts = loadCloudHosts(syncService);

			hostsEventsService.reset();
			servicesEventsService.reset();
			hostsMonitoringService.reset();
			servicesMonitoringService.reset();
			nginxLoadBalancerService.reset();
			registrationServerService.reset();
			workerManagersService.reset();
			zookeeperService.reset();
			kafkaService.reset();
			containersService.reset();
			nodesService.reset();
			elasticIpsService.reset();
			configurationsService.reset();
			heartbeatService.reset();
		};
	}

	@Transactional
	Map<String, ServiceRule> loadServiceRules(ServiceRulesService serviceRulesService,
											  ServiceRuleConditions serviceRuleConditions,
											  Map<RuleDecisionEnum, Decision> decisions,
											  Map<String, Condition> conditions) {
		Map<String, ServiceRule> serviceRuleMap = new HashMap<>();

		ServiceRule rxOver500000GenericServiceRule;
		try {
			rxOver500000GenericServiceRule = serviceRulesService.getRule("RxOver500000");
		}
		catch (EntityNotFoundException ignored) {
			Decision serviceDecisionReplicate = decisions.get(RuleDecisionEnum.REPLICATE);
			Condition rxBytesPerSecOver500000 = conditions.get("RxBytesPerSecOver500000");
			rxOver500000GenericServiceRule = ServiceRule.builder()
				.name("RxOver500000")
				.priority(1)
				.decision(serviceDecisionReplicate)
				.generic(true)
				.build();
			rxOver500000GenericServiceRule = serviceRulesService.addRule(rxOver500000GenericServiceRule);
			ServiceRuleCondition rxOver500000Condition = ServiceRuleCondition.builder()
				.id(new RuleConditionKey(rxOver500000GenericServiceRule.getId(), rxBytesPerSecOver500000.getId()))
				.rule(rxOver500000GenericServiceRule)
				.condition(rxBytesPerSecOver500000)
				.build();
			serviceRuleConditions.save(rxOver500000Condition);
		}
		serviceRuleMap.put("RxOver500000", rxOver500000GenericServiceRule);

		return serviceRuleMap;
	}

	@Transactional
	Map<String, HostRule> loadRules(HostRulesService hostRulesService, RuleConditionsService ruleConditionsService,
									Map<RuleDecisionEnum, Decision> decisions, Map<String, Condition> conditions) {
		Map<String, HostRule> hostRuleMap = new HashMap<>();

		HostRule cpuAndRamOver90GenericHostRule;
		try {
			cpuAndRamOver90GenericHostRule = hostRulesService.getRule("CpuAndRamOver90");
		}
		catch (EntityNotFoundException ignored) {
			Decision hostDecisionOverwork = decisions.get(RuleDecisionEnum.OVERWORK);
			cpuAndRamOver90GenericHostRule = HostRule.builder()
				.name("CpuAndRamOver90")
				.priority(1)
				.decision(hostDecisionOverwork)
				.generic(true)
				.build();
			cpuAndRamOver90GenericHostRule = hostRulesService.addRule(cpuAndRamOver90GenericHostRule);

			Condition cpuPercentageOver90 = conditions.get("CpuPercentageOver90");
			HostRuleCondition cpuOver90Condition = HostRuleCondition.builder()
				.id(new RuleConditionKey(cpuAndRamOver90GenericHostRule.getId(), cpuPercentageOver90.getId()))
				.rule(cpuAndRamOver90GenericHostRule)
				.condition(cpuPercentageOver90)
				.build();
			Condition ramPercentageOver90 = conditions.get("RamPercentageOver90");
			ruleConditionsService.saveHostRuleCondition(cpuOver90Condition);
			HostRuleCondition ramOver90Condition = HostRuleCondition.builder()
				.id(new RuleConditionKey(cpuAndRamOver90GenericHostRule.getId(), ramPercentageOver90.getId()))
				.rule(cpuAndRamOver90GenericHostRule)
				.condition(ramPercentageOver90)
				.build();
			ruleConditionsService.saveHostRuleCondition(ramOver90Condition);
		}
		hostRuleMap.put("CpuAndRamOver90", cpuAndRamOver90GenericHostRule);

		return hostRuleMap;
	}

	@Transactional
	Map<String, Condition> loadConditions(ConditionsService conditionsService, Map<String, ValueMode> valueModes,
										  Map<String, Field> fields, Map<OperatorEnum, Operator> operators) {
		Map<String, Condition> conditionsMap = new HashMap<>();

		ValueMode effectiveValue = valueModes.get("effective-val");
		Operator greaterThan = operators.get(OperatorEnum.GREATER_THAN);

		Condition cpuPercentageOver90;
		try {
			cpuPercentageOver90 = conditionsService.getCondition("CpuPercentageOver90");
		}
		catch (EntityNotFoundException ignored) {
			Field cpuPercentage = fields.get("cpu-%");
			cpuPercentageOver90 = Condition.builder()
				.name("CpuPercentageOver90")
				.valueMode(effectiveValue)
				.field(cpuPercentage)
				.operator(greaterThan)
				.value(90)
				.build();
			cpuPercentageOver90 = conditionsService.addCondition(cpuPercentageOver90);
		}
		conditionsMap.put("CpuPercentageOver90", cpuPercentageOver90);

		Condition ramPercentageOver90;
		try {
			ramPercentageOver90 = conditionsService.getCondition("RamPercentageOver90");
		}
		catch (EntityNotFoundException ignored) {
			Field ramPercentage = fields.get("ram-%");
			ramPercentageOver90 = Condition.builder()
				.name("RamPercentageOver90")
				.valueMode(effectiveValue)
				.field(ramPercentage)
				.operator(greaterThan)
				.value(90)
				.build();
			ramPercentageOver90 = conditionsService.addCondition(ramPercentageOver90);
		}
		conditionsMap.put("RamPercentageOver90", ramPercentageOver90);

		Condition rxBytesPerSecOver500000;
		try {
			rxBytesPerSecOver500000 = conditionsService.getCondition("RxBytesPerSecOver500000");
		}
		catch (EntityNotFoundException ignored) {
			Field rxBytesPerSec = fields.get("rx-bytes-per-sec");
			rxBytesPerSecOver500000 = Condition.builder()
				.name("RxBytesPerSecOver500000")
				.valueMode(effectiveValue)
				.field(rxBytesPerSec)
				.operator(greaterThan)
				.value(500000)
				.build();
			rxBytesPerSecOver500000 = conditionsService.addCondition(rxBytesPerSecOver500000);
		}
		conditionsMap.put("RxBytesPerSecOver500000", rxBytesPerSecOver500000);

		Condition txBytesPerSecOver100000;
		try {
			txBytesPerSecOver100000 = conditionsService.getCondition("TxBytesPerSecOver100000");
		}
		catch (EntityNotFoundException ignored) {
			Field txBytesPerSec = fields.get("tx-bytes-per-sec");
			txBytesPerSecOver100000 = Condition.builder()
				.name("TxBytesPerSecOver100000")
				.valueMode(effectiveValue)
				.field(txBytesPerSec)
				.operator(greaterThan)
				.value(100000)
				.build();
			txBytesPerSecOver100000 = conditionsService.addCondition(txBytesPerSecOver100000);
		}
		conditionsMap.put("TxBytesPerSecOver100000", txBytesPerSecOver100000);

		return conditionsMap;
	}

	@Transactional
	Map<String, ValueMode> loadValueModes(ValueModesService valueModesService) {
		Map<String, ValueMode> valueModes = new HashMap<>();

		ValueMode effectiveValue;
		try {
			effectiveValue = valueModesService.getValueMode("effective-val");
		}
		catch (EntityNotFoundException ignored) {
			effectiveValue = ValueMode.builder()
				.name("effective-val")
				.build();
			effectiveValue = valueModesService.addValueMode(effectiveValue);
		}
		valueModes.put("effective-val", effectiveValue);

		ValueMode averageValue;
		try {
			averageValue = valueModesService.getValueMode("avg-val");
		}
		catch (EntityNotFoundException ignored) {
			averageValue = ValueMode.builder()
				.name("avg-val")
				.build();
			averageValue = valueModesService.addValueMode(averageValue);
		}
		valueModes.put("avg-val", averageValue);

		ValueMode deviationPercentageOnAverageValue;
		try {
			deviationPercentageOnAverageValue = valueModesService.getValueMode("deviation-%-on-avg-val");
		}
		catch (EntityNotFoundException ignored) {
			deviationPercentageOnAverageValue = ValueMode.builder()
				.name("deviation-%-on-avg-val")
				.build();
			deviationPercentageOnAverageValue = valueModesService.addValueMode(deviationPercentageOnAverageValue);
		}
		valueModes.put("deviation-%-on-avg-val", deviationPercentageOnAverageValue);

		ValueMode deviationPercentageOnLastValue;
		try {
			deviationPercentageOnLastValue = valueModesService.getValueMode("deviation-%-on-last-val");
		}
		catch (EntityNotFoundException ignored) {
			deviationPercentageOnLastValue = ValueMode.builder()
				.name("deviation-%-on-last-val")
				.build();
			deviationPercentageOnLastValue = valueModesService.addValueMode(deviationPercentageOnLastValue);
		}
		valueModes.put("deviation-%-on-last-val", deviationPercentageOnLastValue);

		return valueModes;
	}

	@Transactional
	Map<String, Field> loadFields(FieldsService fieldsService) {
		Map<String, Field> fieldsMap = new HashMap<>();

		Field cpu;
		try {
			cpu = fieldsService.getField("cpu");
		}
		catch (EntityNotFoundException ignored) {
			cpu = Field.builder()
				.name("cpu")
				.build();
			cpu = fieldsService.addField(cpu);
		}
		fieldsMap.put("cpu", cpu);

		Field ram;
		try {
			ram = fieldsService.getField("ram");
		}
		catch (EntityNotFoundException ignored) {
			ram = Field.builder()
				.name("ram")
				.prometheusQuery(PrometheusQueryEnum.MEMORY_USAGE)
				.build();
			ram = fieldsService.addField(ram);
		}
		fieldsMap.put("ram", ram);

		Field cpuPercentage;
		try {
			cpuPercentage = fieldsService.getField("cpu-%");
		}
		catch (EntityNotFoundException ignored) {
			cpuPercentage = Field.builder()
				.name("cpu-%")
				.prometheusQuery(PrometheusQueryEnum.CPU_USAGE_PERCENTAGE)
				.build();
			cpuPercentage = fieldsService.addField(cpuPercentage);
		}
		fieldsMap.put("cpu-%", cpuPercentage);

		Field ramPercentage;
		try {
			ramPercentage = fieldsService.getField("ram-%");
		}
		catch (EntityNotFoundException ignored) {
			ramPercentage = Field.builder()
				.name("ram-%")
				.prometheusQuery(PrometheusQueryEnum.MEMORY_USAGE_PERCENTAGE)
				.build();
			ramPercentage = fieldsService.addField(ramPercentage);
		}
		fieldsMap.put("ram-%", ramPercentage);

		Field rxBytes;
		try {
			rxBytes = fieldsService.getField("rx-bytes");
		}
		catch (EntityNotFoundException ignored) {
			rxBytes = Field.builder()
				.name("rx-bytes")
				.build();
			rxBytes = fieldsService.addField(rxBytes);
		}
		fieldsMap.put("rx-bytes", rxBytes);

		Field txBytes;
		try {
			txBytes = fieldsService.getField("tx-bytes");
		}
		catch (EntityNotFoundException ignored) {
			txBytes = Field.builder()
				.name("tx-bytes")
				.build();
			txBytes = fieldsService.addField(txBytes);
		}
		fieldsMap.put("tx-bytes", txBytes);

		Field rxBytesPerSec;
		try {
			rxBytesPerSec = fieldsService.getField("rx-bytes-per-sec");
		}
		catch (EntityNotFoundException ignored) {
			rxBytesPerSec = Field.builder()
				.name("rx-bytes-per-sec")
				.build();
			rxBytesPerSec = fieldsService.addField(rxBytesPerSec);
		}
		fieldsMap.put("rx-bytes-per-sec", rxBytesPerSec);

		Field txBytesPerSec;
		try {
			txBytesPerSec = fieldsService.getField("tx-bytes-per-sec");
		}
		catch (EntityNotFoundException ignored) {
			txBytesPerSec = Field.builder()
				.name("tx-bytes-per-sec")
				.build();
			txBytesPerSec = fieldsService.addField(txBytesPerSec);
		}
		fieldsMap.put("tx-bytes-per-sec", txBytesPerSec);

		Field latency;
		try {
			latency = fieldsService.getField("latency");
		}
		catch (EntityNotFoundException ignored) {
			latency = Field.builder()
				.name("latency")
				.build();
			latency = fieldsService.addField(latency);
		}
		fieldsMap.put("latency", latency);

		Field bandwidthPercentage;
		try {
			bandwidthPercentage = fieldsService.getField("bandwidth-%");
		}
		catch (EntityNotFoundException ignored) {
			bandwidthPercentage = Field.builder()
				.name("bandwidth-%")
				.build();
			bandwidthPercentage = fieldsService.addField(bandwidthPercentage);
		}
		fieldsMap.put("bandwidth-%", bandwidthPercentage);

		Field filesystemAvailableSpace;
		try {
			filesystemAvailableSpace = fieldsService.getField("filesystem-available-space");
		}
		catch (EntityNotFoundException ignored) {
			latency = Field.builder()
				.name("filesystem-available-space")
				.prometheusQuery(PrometheusQueryEnum.FILESYSTEM_AVAILABLE_SPACE)
				.build();
			filesystemAvailableSpace = fieldsService.addField(latency);
		}
		fieldsMap.put("filesystem-available-space", filesystemAvailableSpace);

		return fieldsMap;
	}

	@Transactional
	Map<RuleDecisionEnum, Decision> loadDecisions(DecisionsService decisionsService, Map<ComponentTypeEnum, ComponentType> componentTypes) {
		Map<RuleDecisionEnum, Decision> decisionsMap = new HashMap<>();

		ComponentType serviceComponentType = componentTypes.get(ComponentTypeEnum.SERVICE);

		Decision serviceDecisionNone;
		try {
			serviceDecisionNone = decisionsService.getServicePossibleDecision(RuleDecisionEnum.NONE);
		}
		catch (EntityNotFoundException ignored) {
			serviceDecisionNone = pt.unl.fct.miei.usmanagement.manager.rulesystem.decision.Decision.builder()
				.componentType(serviceComponentType)
				.ruleDecision(RuleDecisionEnum.NONE)
				.build();
			serviceDecisionNone = decisionsService.addDecision(serviceDecisionNone);
		}
		decisionsMap.put(RuleDecisionEnum.NONE, serviceDecisionNone);

		Decision serviceDecisionReplicate;
		try {
			serviceDecisionReplicate = decisionsService.getServicePossibleDecision(RuleDecisionEnum.REPLICATE);
		}
		catch (EntityNotFoundException ignored) {
			serviceDecisionReplicate = pt.unl.fct.miei.usmanagement.manager.rulesystem.decision.Decision.builder()
				.componentType(serviceComponentType)
				.ruleDecision(RuleDecisionEnum.REPLICATE)
				.build();
			serviceDecisionReplicate = decisionsService.addDecision(serviceDecisionReplicate);
		}
		decisionsMap.put(RuleDecisionEnum.REPLICATE, serviceDecisionReplicate);

		Decision serviceDecisionMigrate;
		try {
			serviceDecisionMigrate = decisionsService.getServicePossibleDecision(RuleDecisionEnum.MIGRATE);
		}
		catch (EntityNotFoundException ignored) {
			serviceDecisionMigrate = pt.unl.fct.miei.usmanagement.manager.rulesystem.decision.Decision.builder()
				.componentType(serviceComponentType)
				.ruleDecision(RuleDecisionEnum.MIGRATE)
				.build();
			serviceDecisionMigrate = decisionsService.addDecision(serviceDecisionMigrate);
		}
		decisionsMap.put(RuleDecisionEnum.MIGRATE, serviceDecisionMigrate);

		Decision serviceDecisionStop;
		try {
			serviceDecisionStop = decisionsService.getServicePossibleDecision(RuleDecisionEnum.STOP);
		}
		catch (EntityNotFoundException ignored) {
			serviceDecisionStop = pt.unl.fct.miei.usmanagement.manager.rulesystem.decision.Decision.builder()
				.componentType(serviceComponentType)
				.ruleDecision(RuleDecisionEnum.STOP)
				.build();
			serviceDecisionStop = decisionsService.addDecision(serviceDecisionStop);
		}
		decisionsMap.put(RuleDecisionEnum.STOP, serviceDecisionStop);

		ComponentType hostComponentType = componentTypes.get(ComponentTypeEnum.HOST);

		Decision hostDecisionNone;
		try {
			hostDecisionNone = decisionsService.getHostPossibleDecision(RuleDecisionEnum.NONE);
		}
		catch (EntityNotFoundException ignored) {
			hostDecisionNone = Decision.builder()
				.componentType(hostComponentType)
				.ruleDecision(RuleDecisionEnum.NONE)
				.build();
			hostDecisionNone = decisionsService.addDecision(hostDecisionNone);
		}
		decisionsMap.put(RuleDecisionEnum.NONE, hostDecisionNone);

		Decision hostDecisionStart;
		try {
			hostDecisionStart = decisionsService.getHostPossibleDecision(RuleDecisionEnum.OVERWORK);
		}
		catch (EntityNotFoundException ignored) {
			hostDecisionStart = pt.unl.fct.miei.usmanagement.manager.rulesystem.decision.Decision.builder()
				.componentType(hostComponentType)
				.ruleDecision(RuleDecisionEnum.OVERWORK)
				.build();
			hostDecisionStart = decisionsService.addDecision(hostDecisionStart);
		}
		decisionsMap.put(RuleDecisionEnum.OVERWORK, hostDecisionStart);

		Decision hostDecisionUnderwork;
		try {
			hostDecisionUnderwork = decisionsService.getHostPossibleDecision(RuleDecisionEnum.UNDERWORK);
		}
		catch (EntityNotFoundException ignored) {
			hostDecisionUnderwork = pt.unl.fct.miei.usmanagement.manager.rulesystem.decision.Decision.builder()
				.componentType(hostComponentType)
				.ruleDecision(RuleDecisionEnum.UNDERWORK)
				.build();
			hostDecisionUnderwork = decisionsService.addDecision(hostDecisionUnderwork);
		}
		decisionsMap.put(RuleDecisionEnum.UNDERWORK, hostDecisionUnderwork);

		return decisionsMap;
	}

	@Transactional
	Map<OperatorEnum, Operator> loadOperators(OperatorsService operatorsService) {
		Map<OperatorEnum, Operator> operatorsMap = new HashMap<>();

		Operator notEqualTo;
		try {
			notEqualTo = operatorsService.getOperator(OperatorEnum.NOT_EQUAL_TO);
		}
		catch (EntityNotFoundException ignored) {
			notEqualTo = Operator.builder()
				.operator(OperatorEnum.NOT_EQUAL_TO)
				.symbol(OperatorEnum.NOT_EQUAL_TO.getSymbol())
				.build();
			notEqualTo = operatorsService.addOperator(notEqualTo);
		}
		operatorsMap.put(OperatorEnum.NOT_EQUAL_TO, notEqualTo);

		Operator equalTo;
		try {
			equalTo = operatorsService.getOperator(OperatorEnum.EQUAL_TO);
		}
		catch (EntityNotFoundException ignored) {
			equalTo = Operator.builder()
				.operator(OperatorEnum.EQUAL_TO)
				.symbol(OperatorEnum.EQUAL_TO.getSymbol())
				.build();
			equalTo = operatorsService.addOperator(equalTo);
		}
		operatorsMap.put(OperatorEnum.EQUAL_TO, equalTo);

		Operator greaterThan;
		try {
			greaterThan = operatorsService.getOperator(OperatorEnum.GREATER_THAN);
		}
		catch (EntityNotFoundException ignored) {
			greaterThan = Operator.builder()
				.operator(OperatorEnum.GREATER_THAN)
				.symbol(OperatorEnum.GREATER_THAN.getSymbol())
				.build();
			greaterThan = operatorsService.addOperator(greaterThan);
		}
		operatorsMap.put(OperatorEnum.GREATER_THAN, greaterThan);

		Operator lessThan;
		try {
			lessThan = operatorsService.getOperator(OperatorEnum.LESS_THAN);
		}
		catch (EntityNotFoundException ignored) {
			lessThan = Operator.builder()
				.operator(OperatorEnum.LESS_THAN)
				.symbol(OperatorEnum.LESS_THAN.getSymbol())
				.build();
			lessThan = operatorsService.addOperator(lessThan);
		}
		operatorsMap.put(OperatorEnum.LESS_THAN, lessThan);

		Operator greaterThanOrEqualTo;
		try {
			greaterThanOrEqualTo = operatorsService.getOperator(OperatorEnum.GREATER_THAN_OR_EQUAL_TO);
		}
		catch (EntityNotFoundException ignored) {
			greaterThanOrEqualTo = Operator.builder()
				.operator(OperatorEnum.GREATER_THAN_OR_EQUAL_TO)
				.symbol(OperatorEnum.GREATER_THAN_OR_EQUAL_TO.getSymbol())
				.build();
			greaterThanOrEqualTo = operatorsService.addOperator(greaterThanOrEqualTo);
		}
		operatorsMap.put(OperatorEnum.GREATER_THAN_OR_EQUAL_TO, greaterThanOrEqualTo);

		Operator lessThanOrEqualTo;
		try {
			lessThanOrEqualTo = operatorsService.getOperator(OperatorEnum.LESS_THAN_OR_EQUAL_TO);
		}
		catch (EntityNotFoundException ignored) {
			lessThanOrEqualTo = Operator.builder()
				.operator(OperatorEnum.LESS_THAN_OR_EQUAL_TO)
				.symbol(OperatorEnum.LESS_THAN_OR_EQUAL_TO.getSymbol())
				.build();
			lessThanOrEqualTo = operatorsService.addOperator(lessThanOrEqualTo);
		}
		operatorsMap.put(OperatorEnum.LESS_THAN_OR_EQUAL_TO, lessThanOrEqualTo);

		return operatorsMap;
	}

	@Transactional
	Map<ComponentTypeEnum, ComponentType> loadComponentTypes(ComponentTypesService componentTypesService) {
		Map<ComponentTypeEnum, ComponentType> componentTypesMap = new HashMap<>();

		ComponentType hostComponentType;
		try {
			hostComponentType = componentTypesService.getComponentType(ComponentTypeEnum.HOST);
		}
		catch (EntityNotFoundException ignored) {
			hostComponentType = ComponentType.builder()
				.type(ComponentTypeEnum.HOST)
				.build();
			hostComponentType = componentTypesService.addComponentType(hostComponentType);
		}
		componentTypesMap.put(ComponentTypeEnum.HOST, hostComponentType);

		ComponentType serviceComponentType;
		try {
			serviceComponentType = componentTypesService.getComponentType(ComponentTypeEnum.SERVICE);
		}
		catch (EntityNotFoundException ignored) {
			serviceComponentType = ComponentType.builder()
				.type(ComponentTypeEnum.SERVICE)
				.build();
			serviceComponentType = componentTypesService.addComponentType(serviceComponentType);
		}
		componentTypesMap.put(ComponentTypeEnum.SERVICE, serviceComponentType);

		ComponentType container;
		try {
			container = componentTypesService.getComponentType(ComponentTypeEnum.CONTAINER);
		}
		catch (EntityNotFoundException ignored) {
			container = ComponentType.builder()
				.type(ComponentTypeEnum.CONTAINER)
				.build();
			container = componentTypesService.addComponentType(container);
		}
		componentTypesMap.put(ComponentTypeEnum.CONTAINER, container);

		return componentTypesMap;
	}

	@Transactional
	List<CloudHost> loadCloudHosts(SyncService syncService) {
		return syncService.synchronizeCloudHostsDatabase();
	}

	@Transactional
	List<EdgeHost> loadEdgeHosts(EdgeHostsService edgeHostsService) {
		List<EdgeHost> egeHosts = new ArrayList<>(1);

        /*EdgeHost danielHost;
        try {
            danielHost = edgeHostsService.getEdgeHostByDns("danielfct.ddns.net");
        } catch (EntityNotFoundException ignored) {
            Coordinates coordinates = new Coordinates("Portugal", 39.575097, -8.909794);
            RegionEnum region = RegionEnum.getClosestRegion(coordinates);
            danielHost = edgeHostsService.addEdgeHost(EdgeHost.builder()
                    .username("daniel")
                    .publicIpAddress("2.82.208.89")
                    .privateIpAddress("192.168.1.83")
                    .publicDnsName("danielfct.ddns.net")
                    .region(region)
                    .coordinates(coordinates)
                    .local(true)
                    .build());
        }
        egeHosts.add(danielHost);*/

		EdgeHost danielHost2;
		try {
			danielHost2 = edgeHostsService.getEdgeHostByAddress(new HostAddress("2.82.208.89", "192.168.1.94"));
		}
		catch (EntityNotFoundException ignored) {
			Coordinates coordinates = new Coordinates("Portugal", 39.575097, -8.909794);
			RegionEnum region = RegionEnum.getClosestRegion(coordinates);
			danielHost2 = edgeHostsService.addEdgeHost(EdgeHost.builder()
				.username("daniel")
				.publicIpAddress("2.82.208.89")
				.privateIpAddress("192.168.1.94")
				.region(region)
				.coordinates(coordinates)
				.local(true)
				.build());
		}
		egeHosts.add(danielHost2);

		return egeHosts;
	}


	/*@Transactional
    Map<RegionEnum, Region> loadRegions(RegionsService regionsService) {
		Map<RegionEnum, Region> regionsMap = new HashMap<>();

		for (RegionEnum regionEnum : RegionEnum.values()) {
			Region region;
			try {
				region = regionsService.getRegion(regionEnum);
			} catch (EntityNotFoundException ignored) {
				region = Region.builder()
					.region(region)
					.active(true)
					.build();
				region = regionsService.addRegion(region);
			}

			regionsMap.put(regionEnum, region);
		}

		return regionsMap;
	}*/

	@Transactional
	Map.Entry<String, Service> associateServiceToApp(String appName, String serviceName, Integer defaultExternalPort,
													 Integer defaultInternalPort, ServiceTypeEnum type,
													 Set<String> environment, ServicesService servicesService,
													 String dockerHubUsername, List<String> launch,
													 Double expectedMemoryConsumption) {
		String app = appName.toLowerCase().replace(" ", "-");
		String repositoryName = app + "-" + serviceName.toLowerCase().replace(" ", "-");
		serviceName = serviceName.equalsIgnoreCase("frontend") ? app : repositoryName;
		Service service;
		try {
			service = servicesService.getService(serviceName);
		}
		catch (EntityNotFoundException ignored) {
			String launchCommand = launch == null ? null : launch.stream().map(param -> "${" + app + "-" + param + "Host}").collect(Collectors.joining(" "));
			service = Service.builder()
				.serviceName(serviceName)
				.dockerRepository(dockerHubUsername + "/" + repositoryName)
				.defaultExternalPort(defaultExternalPort)
				.defaultInternalPort(defaultInternalPort)
				.launchCommand(type == ServiceTypeEnum.DATABASE
					? null
					: String.format("${externalPort} ${internalPort} ${hostname} ${registrationClientPort} ${latitude} ${longitude}%s", launchCommand != null ? " " + launchCommand : ""))
				.minimumReplicas(1)
				.outputLabel(String.format("${%sHost}", serviceName))
				.environment(environment)
				.serviceType(type)
				.expectedMemoryConsumption(expectedMemoryConsumption)
				.build();
			service = servicesService.addService(service);
		}
		return Map.entry(repositoryName, service);
	}

	@Transactional
	Map<String, Service> loadTestingSuite(String dockerHubUsername, AppsService appsService,
										  ServicesService servicesService, AppServices appServices) {
		Map<String, Service> servicesMap = new HashMap<>(1);

		String appName = "Test Suite";

		Map.Entry<String, Service> crashTesting = associateServiceToApp(appName, "crash-testing", 2500, 80, ServiceTypeEnum.BACKEND,
			Collections.emptySet(), servicesService, dockerHubUsername, null, null);
		servicesMap.put(crashTesting.getKey(), crashTesting.getValue());

		if (!appsService.hasApp(appName)) {
			App testing = App.builder().name(appName)
				.description("Microservices designed to test components of the system.")
				.build();
			testing = appsService.addApp(testing);
			appServices.saveAll(List.of(
				AppService.builder().id(new AppServiceKey(testing.getId(), crashTesting.getValue().getServiceName())).app(testing).service(crashTesting.getValue()).build())
			);
		}

		return servicesMap;
	}

	@Transactional
	Map<String, Service> loadSocialNetwork(String dockerHubUsername, AppsService appsService, ServicesService servicesService,
										   ServiceDependenciesService serviceDependenciesService, AppServices appServices) {
		Map<String, Service> servicesMap = new HashMap<>();

		String appName = "Social Network";

		Map.Entry<String, Service> composePost = associateServiceToApp(appName, "compose-post", 9092, 9092, ServiceTypeEnum.BACKEND,
			Collections.emptySet(), servicesService, dockerHubUsername, List.of("compose-post-redis", "rabbitmq"), null);
		servicesMap.put(composePost.getKey(), composePost.getValue());

		Map.Entry<String, Service> composePostRedis = associateServiceToApp(appName, "compose-post-redis", 6379, 6379, ServiceTypeEnum.DATABASE,
			Collections.emptySet(), servicesService, dockerHubUsername, null, null);
		servicesMap.put(composePostRedis.getKey(), composePostRedis.getValue());

		Map.Entry<String, Service> readHomeTimeline = associateServiceToApp(appName, "read-home-timeline", 9101, 9101, ServiceTypeEnum.BACKEND,
			Collections.emptySet(), servicesService, dockerHubUsername, List.of("home-timeline-redis"), null);
		servicesMap.put(readHomeTimeline.getKey(), readHomeTimeline.getValue());

		Map.Entry<String, Service> media = associateServiceToApp(appName, "media", 9096, 9096, ServiceTypeEnum.BACKEND,
			Collections.emptySet(), servicesService, dockerHubUsername, List.of("media-memcached", "media-mongodb"), null);
		servicesMap.put(media.getKey(), media.getValue());

		Map.Entry<String, Service> mediaMemcached = associateServiceToApp(appName, "media-memcached", 11211, 11211, ServiceTypeEnum.DATABASE,
			Collections.emptySet(), servicesService, dockerHubUsername, null, null);
		servicesMap.put(mediaMemcached.getKey(), mediaMemcached.getValue());

		Map.Entry<String, Service> mediaMongodb = associateServiceToApp(appName, "media-mongodb", 27017, 27017, ServiceTypeEnum.DATABASE,
			Collections.emptySet(), servicesService, dockerHubUsername, null, null);
		servicesMap.put(mediaMongodb.getKey(), mediaMongodb.getValue());

		Map.Entry<String, Service> postStorage = associateServiceToApp(appName, "post-storage", 9094, 9094, ServiceTypeEnum.BACKEND,
			Collections.emptySet(), servicesService, dockerHubUsername, List.of("post-storage-memcached", "post-storage-mongodb"), null);
		servicesMap.put(postStorage.getKey(), postStorage.getValue());

		Map.Entry<String, Service> postStorageMemcached = associateServiceToApp(appName, "post-storage-memcached", 11211, 11211, ServiceTypeEnum.DATABASE,
			Collections.emptySet(), servicesService, dockerHubUsername, null, null);
		servicesMap.put(postStorageMemcached.getKey(), postStorageMemcached.getValue());

		Map.Entry<String, Service> postStorageMongodb = associateServiceToApp(appName, "post-storage-mongodb", 27017, 27017, ServiceTypeEnum.DATABASE,
			Collections.emptySet(), servicesService, dockerHubUsername, null, null);
		servicesMap.put(postStorageMongodb.getKey(), postStorageMongodb.getValue());

		Map.Entry<String, Service> socialGraph = associateServiceToApp(appName, "social-graph", 9090, 9090, ServiceTypeEnum.BACKEND,
			Collections.emptySet(), servicesService, dockerHubUsername, List.of("social-graph-memcached", "social-graph-mongodb"), null);
		servicesMap.put(socialGraph.getKey(), socialGraph.getValue());

		Map.Entry<String, Service> socialGraphMemcached = associateServiceToApp(appName, "social-graph-memcached", 11211, 11211, ServiceTypeEnum.DATABASE,
			Collections.emptySet(), servicesService, dockerHubUsername, null, null);
		servicesMap.put(socialGraphMemcached.getKey(), socialGraphMemcached.getValue());

		Map.Entry<String, Service> socialGraphMongodb = associateServiceToApp(appName, "social-graph-mongodb", 27017, 27017, ServiceTypeEnum.DATABASE,
			Collections.emptySet(), servicesService, dockerHubUsername, null, null);
		servicesMap.put(socialGraphMongodb.getKey(), socialGraphMongodb.getValue());

		Map.Entry<String, Service> text = associateServiceToApp(appName, "text", 9097, 9097, ServiceTypeEnum.BACKEND,
			Collections.emptySet(), servicesService, dockerHubUsername, null, null);
		servicesMap.put(text.getKey(), text.getValue());

		Map.Entry<String, Service> uniqueId = associateServiceToApp(appName, "unique-id", 9095, 9095, ServiceTypeEnum.BACKEND,
			Collections.emptySet(), servicesService, dockerHubUsername, null, null);
		servicesMap.put(uniqueId.getKey(), uniqueId.getValue());

		Map.Entry<String, Service> urlShorten = associateServiceToApp(appName, "url-shorten", 9099, 9099, ServiceTypeEnum.BACKEND,
			Collections.emptySet(), servicesService, dockerHubUsername, List.of("url-shorten-memcached", "url-shorten-mongodb"), null);
		servicesMap.put(urlShorten.getKey(), urlShorten.getValue());

		Map.Entry<String, Service> urlShortenMemcached = associateServiceToApp(appName, "url-shorten-memcached", 11211, 11211, ServiceTypeEnum.DATABASE,
			Collections.emptySet(), servicesService, dockerHubUsername, null, null);
		servicesMap.put(urlShortenMemcached.getKey(), urlShortenMemcached.getValue());

		Map.Entry<String, Service> urlShortenMongodb = associateServiceToApp(appName, "url-shorten-mongodb", 27017, 27017, ServiceTypeEnum.DATABASE,
			Collections.emptySet(), servicesService, dockerHubUsername, null, null);
		servicesMap.put(urlShortenMongodb.getKey(), urlShortenMongodb.getValue());

		Map.Entry<String, Service> userTag = associateServiceToApp(appName, "user-tag", 9098, 9098, ServiceTypeEnum.BACKEND,
			Collections.emptySet(), servicesService, dockerHubUsername, null, null);
		servicesMap.put(userTag.getKey(), userTag.getValue());

		Map.Entry<String, Service> user = associateServiceToApp(appName, "user", 9100, 9100, ServiceTypeEnum.BACKEND,
			Collections.emptySet(), servicesService, dockerHubUsername, List.of("user-memcached", "user-mongodb"), null);
		servicesMap.put(user.getKey(), user.getValue());

		Map.Entry<String, Service> userMemcached = associateServiceToApp(appName, "user-memcached", 11211, 11211, ServiceTypeEnum.DATABASE,
			Collections.emptySet(), servicesService, dockerHubUsername, null, null);
		servicesMap.put(userMemcached.getKey(), userMemcached.getValue());

		Map.Entry<String, Service> userMongodb = associateServiceToApp(appName, "user-mongodb", 27017, 27017, ServiceTypeEnum.DATABASE,
			Collections.emptySet(), servicesService, dockerHubUsername, null, null);
		servicesMap.put(userMongodb.getKey(), userMongodb.getValue());

		Map.Entry<String, Service> userTimeline = associateServiceToApp(appName, "user-timeline", 9093, 9093, ServiceTypeEnum.BACKEND,
			Collections.emptySet(), servicesService, dockerHubUsername, List.of("user-timeline-redis", "user-timeline-mongodb"), null);
		servicesMap.put(userTimeline.getKey(), userTimeline.getValue());

		Map.Entry<String, Service> userTimelineRedis = associateServiceToApp(appName, "user-timeline-redis", 6379, 6379, ServiceTypeEnum.DATABASE,
			Collections.emptySet(), servicesService, dockerHubUsername, null, null);
		servicesMap.put(userTimelineRedis.getKey(), userTimelineRedis.getValue());

		Map.Entry<String, Service> userTimelineMongodb = associateServiceToApp(appName, "user-timeline-mongodb", 27017, 27017, ServiceTypeEnum.DATABASE,
			Collections.emptySet(), servicesService, dockerHubUsername, null, null);
		servicesMap.put(userTimelineMongodb.getKey(), userTimelineMongodb.getValue());

		Map.Entry<String, Service> writeHomeTimeline = associateServiceToApp(appName, "write-home-timeline", 9091, 9091, ServiceTypeEnum.BACKEND,
			Collections.emptySet(), servicesService, dockerHubUsername, List.of("home-timeline-redis", "rabbitmq"), null);
		servicesMap.put(writeHomeTimeline.getKey(), writeHomeTimeline.getValue());

		Map.Entry<String, Service> homeTimelineRedis = associateServiceToApp(appName, "home-timeline-redis", 6379, 6379, ServiceTypeEnum.DATABASE,
			Collections.emptySet(), servicesService, dockerHubUsername, null, null);
		servicesMap.put(homeTimelineRedis.getKey(), homeTimelineRedis.getValue());

		Map.Entry<String, Service> rabbitmq = associateServiceToApp(appName, "rabbitmq", 5672, 5672, ServiceTypeEnum.BACKEND,
			Collections.emptySet(), servicesService, dockerHubUsername, null, 262144000d);
		servicesMap.put(rabbitmq.getKey(), rabbitmq.getValue());

		Map.Entry<String, Service> nginx = associateServiceToApp(appName, "nginx", 8080, 8080, ServiceTypeEnum.BACKEND,
			Collections.emptySet(), servicesService, dockerHubUsername, null, null);
		servicesMap.put(nginx.getKey(), nginx.getValue());

		Map.Entry<String, Service> frontend = associateServiceToApp(appName, "frontend", 9089, 9089, ServiceTypeEnum.FRONTEND,
			Collections.emptySet(), servicesService, dockerHubUsername, null, null);
		servicesMap.put(frontend.getKey(), frontend.getValue());

		if (!appsService.hasApp(appName)) {
			App socialNetwork = App.builder().name(appName)
				.description("A social network with unidirectional follow relationships, implemented with loosely-coupled microservices, communicating with each other via Thrift RPCs.")
				.build();
			socialNetwork = appsService.addApp(socialNetwork);
			appServices.saveAll(List.of(
				AppService.builder().id(new AppServiceKey(socialNetwork.getId(), composePost.getValue().getServiceName())).app(socialNetwork).service(composePost.getValue()).launchOrder(1).build(),
				AppService.builder().id(new AppServiceKey(socialNetwork.getId(), composePostRedis.getValue().getServiceName())).app(socialNetwork).service(composePostRedis.getValue()).launchOrder(2).build(),
				AppService.builder().id(new AppServiceKey(socialNetwork.getId(), readHomeTimeline.getValue().getServiceName())).app(socialNetwork).service(readHomeTimeline.getValue()).launchOrder(3).build(),
				AppService.builder().id(new AppServiceKey(socialNetwork.getId(), media.getValue().getServiceName())).app(socialNetwork).service(media.getValue()).launchOrder(4).build(),
				AppService.builder().id(new AppServiceKey(socialNetwork.getId(), mediaMemcached.getValue().getServiceName())).app(socialNetwork).service(mediaMemcached.getValue()).launchOrder(5).build(),
				AppService.builder().id(new AppServiceKey(socialNetwork.getId(), mediaMongodb.getValue().getServiceName())).app(socialNetwork).service(mediaMongodb.getValue()).launchOrder(6).build(),
				AppService.builder().id(new AppServiceKey(socialNetwork.getId(), postStorage.getValue().getServiceName())).app(socialNetwork).service(postStorage.getValue()).launchOrder(7).build(),
				AppService.builder().id(new AppServiceKey(socialNetwork.getId(), postStorageMemcached.getValue().getServiceName())).app(socialNetwork).service(postStorageMemcached.getValue()).launchOrder(8).build(),
				AppService.builder().id(new AppServiceKey(socialNetwork.getId(), postStorageMongodb.getValue().getServiceName())).app(socialNetwork).service(postStorageMongodb.getValue()).launchOrder(9).build(),
				AppService.builder().id(new AppServiceKey(socialNetwork.getId(), socialGraph.getValue().getServiceName())).app(socialNetwork).service(socialGraph.getValue()).launchOrder(10).build(),
				AppService.builder().id(new AppServiceKey(socialNetwork.getId(), socialGraphMemcached.getValue().getServiceName())).app(socialNetwork).service(socialGraphMemcached.getValue()).launchOrder(11).build(),
				AppService.builder().id(new AppServiceKey(socialNetwork.getId(), socialGraphMongodb.getValue().getServiceName())).app(socialNetwork).service(socialGraphMongodb.getValue()).launchOrder(12).build(),
				AppService.builder().id(new AppServiceKey(socialNetwork.getId(), text.getValue().getServiceName())).app(socialNetwork).service(text.getValue()).launchOrder(13).build(),
				AppService.builder().id(new AppServiceKey(socialNetwork.getId(), uniqueId.getValue().getServiceName())).app(socialNetwork).service(uniqueId.getValue()).launchOrder(14).build(),
				AppService.builder().id(new AppServiceKey(socialNetwork.getId(), urlShorten.getValue().getServiceName())).app(socialNetwork).service(urlShorten.getValue()).launchOrder(15).build(),
				AppService.builder().id(new AppServiceKey(socialNetwork.getId(), urlShortenMemcached.getValue().getServiceName())).app(socialNetwork).service(urlShortenMemcached.getValue()).launchOrder(16).build(),
				AppService.builder().id(new AppServiceKey(socialNetwork.getId(), urlShortenMongodb.getValue().getServiceName())).app(socialNetwork).service(urlShortenMongodb.getValue()).launchOrder(17).build(),
				AppService.builder().id(new AppServiceKey(socialNetwork.getId(), userTag.getValue().getServiceName())).app(socialNetwork).service(userTag.getValue()).launchOrder(18).build(),
				AppService.builder().id(new AppServiceKey(socialNetwork.getId(), user.getValue().getServiceName())).app(socialNetwork).service(user.getValue()).launchOrder(19).build(),
				AppService.builder().id(new AppServiceKey(socialNetwork.getId(), userMemcached.getValue().getServiceName())).app(socialNetwork).service(userMemcached.getValue()).launchOrder(20).build(),
				AppService.builder().id(new AppServiceKey(socialNetwork.getId(), userMongodb.getValue().getServiceName())).app(socialNetwork).service(userMongodb.getValue()).launchOrder(21).build(),
				AppService.builder().id(new AppServiceKey(socialNetwork.getId(), userTimeline.getValue().getServiceName())).app(socialNetwork).service(userTimeline.getValue()).launchOrder(21).build(),
				AppService.builder().id(new AppServiceKey(socialNetwork.getId(), userTimelineRedis.getValue().getServiceName())).app(socialNetwork).service(userTimelineRedis.getValue()).launchOrder(22).build(),
				AppService.builder().id(new AppServiceKey(socialNetwork.getId(), userTimelineMongodb.getValue().getServiceName())).app(socialNetwork).service(userTimelineMongodb.getValue()).launchOrder(23).build(),
				AppService.builder().id(new AppServiceKey(socialNetwork.getId(), writeHomeTimeline.getValue().getServiceName())).app(socialNetwork).service(writeHomeTimeline.getValue()).launchOrder(24).build(),
				AppService.builder().id(new AppServiceKey(socialNetwork.getId(), homeTimelineRedis.getValue().getServiceName())).app(socialNetwork).service(homeTimelineRedis.getValue()).launchOrder(25).build(),
				AppService.builder().id(new AppServiceKey(socialNetwork.getId(), rabbitmq.getValue().getServiceName())).app(socialNetwork).service(rabbitmq.getValue()).launchOrder(26).build(),
				AppService.builder().id(new AppServiceKey(socialNetwork.getId(), nginx.getValue().getServiceName())).app(socialNetwork).service(nginx.getValue()).launchOrder(27).build(),
				AppService.builder().id(new AppServiceKey(socialNetwork.getId(), frontend.getValue().getServiceName())).app(socialNetwork).service(frontend.getValue()).launchOrder(28).build())
			);

			if (!serviceDependenciesService.hasDependency(composePost.getKey(), rabbitmq.getKey())) {
				serviceDependenciesService.addDependency(composePost.getValue(), rabbitmq.getValue());
			}
			if (!serviceDependenciesService.hasDependency(composePost.getKey(), userTimeline.getKey())) {
				serviceDependenciesService.addDependency(composePost.getValue(), userTimeline.getValue());
			}
			if (!serviceDependenciesService.hasDependency(composePost.getKey(), composePostRedis.getKey())) {
				serviceDependenciesService.addDependency(composePost.getValue(), composePostRedis.getValue());
			}
			if (!serviceDependenciesService.hasDependency(readHomeTimeline.getKey(), postStorage.getKey())) {
				serviceDependenciesService.addDependency(readHomeTimeline.getValue(), postStorage.getValue());
			}
			if (!serviceDependenciesService.hasDependency(readHomeTimeline.getKey(), homeTimelineRedis.getKey())) {
				serviceDependenciesService.addDependency(readHomeTimeline.getValue(), homeTimelineRedis.getValue());
			}
			if (!serviceDependenciesService.hasDependency(media.getKey(), composePost.getKey())) {
				serviceDependenciesService.addDependency(media.getValue(), composePost.getValue());
			}
			if (!serviceDependenciesService.hasDependency(media.getKey(), mediaMemcached.getKey())) {
				serviceDependenciesService.addDependency(media.getValue(), mediaMemcached.getValue());
			}
			if (!serviceDependenciesService.hasDependency(media.getKey(), mediaMongodb.getKey())) {
				serviceDependenciesService.addDependency(media.getValue(), mediaMongodb.getValue());
			}
			if (!serviceDependenciesService.hasDependency(postStorage.getKey(), postStorageMemcached.getKey())) {
				serviceDependenciesService.addDependency(postStorage.getValue(), postStorageMemcached.getValue());
			}
			if (!serviceDependenciesService.hasDependency(postStorage.getKey(), postStorageMongodb.getKey())) {
				serviceDependenciesService.addDependency(postStorage.getValue(), postStorageMongodb.getValue());
			}
			if (!serviceDependenciesService.hasDependency(socialGraph.getKey(), user.getKey())) {
				serviceDependenciesService.addDependency(socialGraph.getValue(), user.getValue());
			}
			if (!serviceDependenciesService.hasDependency(socialGraph.getKey(), socialGraphMemcached.getKey())) {
				serviceDependenciesService.addDependency(socialGraph.getValue(), socialGraphMemcached.getValue());
			}
			if (!serviceDependenciesService.hasDependency(socialGraph.getKey(), socialGraphMongodb.getKey())) {
				serviceDependenciesService.addDependency(socialGraph.getValue(), socialGraphMongodb.getValue());
			}
			if (!serviceDependenciesService.hasDependency(text.getKey(), composePost.getKey())) {
				serviceDependenciesService.addDependency(text.getValue(), composePost.getValue());
			}
			if (!serviceDependenciesService.hasDependency(text.getKey(), nginx.getKey())) {
				serviceDependenciesService.addDependency(text.getValue(), nginx.getValue());
			}
			if (!serviceDependenciesService.hasDependency(uniqueId.getKey(), composePost.getKey())) {
				serviceDependenciesService.addDependency(uniqueId.getValue(), composePost.getValue());
			}
			if (!serviceDependenciesService.hasDependency(urlShorten.getKey(), composePost.getKey())) {
				serviceDependenciesService.addDependency(urlShorten.getValue(), composePost.getValue());
			}
			if (!serviceDependenciesService.hasDependency(urlShorten.getKey(), urlShortenMemcached.getKey())) {
				serviceDependenciesService.addDependency(urlShorten.getValue(), urlShortenMemcached.getValue());
			}
			if (!serviceDependenciesService.hasDependency(urlShorten.getKey(), urlShortenMongodb.getKey())) {
				serviceDependenciesService.addDependency(urlShorten.getValue(), urlShortenMongodb.getValue());
			}
			if (!serviceDependenciesService.hasDependency(userTag.getKey(), composePost.getKey())) {
				serviceDependenciesService.addDependency(userTag.getValue(), composePost.getValue());
			}
			if (!serviceDependenciesService.hasDependency(user.getKey(), composePost.getKey())) {
				serviceDependenciesService.addDependency(user.getValue(), composePost.getValue());
			}
			if (!serviceDependenciesService.hasDependency(user.getKey(), userMemcached.getKey())) {
				serviceDependenciesService.addDependency(user.getValue(), userMemcached.getValue());
			}
			if (!serviceDependenciesService.hasDependency(user.getKey(), userMongodb.getKey())) {
				serviceDependenciesService.addDependency(user.getValue(), userMongodb.getValue());
			}
			if (!serviceDependenciesService.hasDependency(userTimeline.getKey(), postStorage.getKey())) {
				serviceDependenciesService.addDependency(userTimeline.getValue(), postStorage.getValue());
			}
			if (!serviceDependenciesService.hasDependency(userTimeline.getKey(), userTimelineRedis.getKey())) {
				serviceDependenciesService.addDependency(userTimeline.getValue(), userTimelineRedis.getValue());
			}
			if (!serviceDependenciesService.hasDependency(userTimeline.getKey(), userTimelineMongodb.getKey())) {
				serviceDependenciesService.addDependency(userTimeline.getValue(), userTimelineMongodb.getValue());
			}
			if (!serviceDependenciesService.hasDependency(writeHomeTimeline.getKey(), homeTimelineRedis.getKey())) {
				serviceDependenciesService.addDependency(writeHomeTimeline.getValue(), homeTimelineRedis.getValue());
			}
			if (!serviceDependenciesService.hasDependency(writeHomeTimeline.getKey(), homeTimelineRedis.getKey())) {
				serviceDependenciesService.addDependency(writeHomeTimeline.getValue(), homeTimelineRedis.getValue());
			}
			if (!serviceDependenciesService.hasDependency(nginx.getKey(), readHomeTimeline.getKey())) {
				serviceDependenciesService.addDependency(nginx.getValue(), readHomeTimeline.getValue());
			}
			if (!serviceDependenciesService.hasDependency(nginx.getKey(), uniqueId.getKey())) {
				serviceDependenciesService.addDependency(nginx.getValue(), uniqueId.getValue());
			}
			if (!serviceDependenciesService.hasDependency(nginx.getKey(), urlShorten.getKey())) {
				serviceDependenciesService.addDependency(nginx.getValue(), urlShorten.getValue());
			}
			if (!serviceDependenciesService.hasDependency(nginx.getKey(), media.getKey())) {
				serviceDependenciesService.addDependency(nginx.getValue(), media.getValue());
			}
			if (!serviceDependenciesService.hasDependency(nginx.getKey(), text.getKey())) {
				serviceDependenciesService.addDependency(nginx.getValue(), text.getValue());
			}
			if (!serviceDependenciesService.hasDependency(nginx.getKey(), userTag.getKey())) {
				serviceDependenciesService.addDependency(nginx.getValue(), userTag.getValue());
			}
			if (!serviceDependenciesService.hasDependency(nginx.getKey(), postStorage.getKey())) {
				serviceDependenciesService.addDependency(nginx.getValue(), postStorage.getValue());
			}
		}

		return servicesMap;
	}

	@Transactional
	Map<String, Service> loadMediaMicroservices(String dockerHubUsername, AppsService appsService, ServicesService servicesService,
												ServiceDependenciesService serviceDependenciesService, AppServices appServices) {
		Map<String, Service> servicesMap = new HashMap<>(32);

		String appName = "Media";

		Map.Entry<String, Service> uniqueId = associateServiceToApp(appName, "unique-id", 10001, 9090, ServiceTypeEnum.BACKEND,
			Collections.emptySet(), servicesService, dockerHubUsername, null, null);
		servicesMap.put(uniqueId.getKey(), uniqueId.getValue());
		Map.Entry<String, Service> movieId = associateServiceToApp(appName, "movie-id", 10002, 9090, ServiceTypeEnum.BACKEND,
			Collections.emptySet(), servicesService, dockerHubUsername, List.of("movie-id-db", "movie-id-memcached"), null);
		servicesMap.put(movieId.getKey(), movieId.getValue());
		Map.Entry<String, Service> movieIdDb = associateServiceToApp(appName, "movie-id-db", 40018, 27017, ServiceTypeEnum.DATABASE,
			Collections.emptySet(), servicesService, dockerHubUsername, null, null);
		servicesMap.put(movieIdDb.getKey(), movieIdDb.getValue());
		Map.Entry<String, Service> movieIdMemcached = associateServiceToApp(appName, "movie-id-memcached", 21212, 11211, ServiceTypeEnum.DATABASE,
			Collections.emptySet(), servicesService, dockerHubUsername, null, null);
		servicesMap.put(movieIdMemcached.getKey(), movieIdMemcached.getValue());
		Map.Entry<String, Service> text = associateServiceToApp(appName, "text", 10003, 9090, ServiceTypeEnum.BACKEND,
			Collections.emptySet(), servicesService, dockerHubUsername, null, null);
		servicesMap.put(text.getKey(), text.getValue());
		Map.Entry<String, Service> rating = associateServiceToApp(appName, "rating", 10004, 9090, ServiceTypeEnum.BACKEND,
			Collections.emptySet(), servicesService, dockerHubUsername, List.of("rating-redis"), null);
		servicesMap.put(rating.getKey(), rating.getValue());
		Map.Entry<String, Service> ratingRedis = associateServiceToApp(appName, "rating-redis", 6382, 6379, ServiceTypeEnum.DATABASE,
			Collections.emptySet(), servicesService, dockerHubUsername, null, null);
		servicesMap.put(ratingRedis.getKey(), ratingRedis.getValue());
		Map.Entry<String, Service> user = associateServiceToApp(appName, "user", 10005, 9090, ServiceTypeEnum.BACKEND,
			Collections.emptySet(), servicesService, dockerHubUsername, List.of("user-db", "user-memcached"), null);
		servicesMap.put(user.getKey(), user.getValue());
		Map.Entry<String, Service> userDb = associateServiceToApp(appName, "user-db", 41018, 27017, ServiceTypeEnum.DATABASE,
			Collections.emptySet(), servicesService, dockerHubUsername, null, null);
		servicesMap.put(userDb.getKey(), userDb.getValue());
		Map.Entry<String, Service> userMemcached = associateServiceToApp(appName, "user-memcached", 21213, 11211, ServiceTypeEnum.DATABASE,
			Collections.emptySet(), servicesService, dockerHubUsername, null, null);
		servicesMap.put(userMemcached.getKey(), userMemcached.getValue());
		Map.Entry<String, Service> composeReview = associateServiceToApp(appName, "compose-review", 10006, 9090, ServiceTypeEnum.BACKEND,
			Collections.emptySet(), servicesService, dockerHubUsername, List.of("compose-review-memcached"), null);
		servicesMap.put(composeReview.getKey(), composeReview.getValue());
		Map.Entry<String, Service> composeReviewMemcached = associateServiceToApp(appName, "compose-review-memcached", 10006, 9090, ServiceTypeEnum.BACKEND,
			Collections.emptySet(), servicesService, dockerHubUsername, null, null);
		servicesMap.put(composeReviewMemcached.getKey(), composeReviewMemcached.getValue());
		Map.Entry<String, Service> reviewStorage = associateServiceToApp(appName, "review-storage", 10007, 9090, ServiceTypeEnum.BACKEND,
			Collections.emptySet(), servicesService, dockerHubUsername, List.of("review-storage-db", "review-storage-memcached"), null);
		servicesMap.put(reviewStorage.getKey(), reviewStorage.getValue());
		Map.Entry<String, Service> reviewStorageDb = associateServiceToApp(appName, "review-storage-db", 42018, 27017, ServiceTypeEnum.DATABASE,
			Collections.emptySet(), servicesService, dockerHubUsername, null, null);
		servicesMap.put(reviewStorageDb.getKey(), reviewStorageDb.getValue());
		Map.Entry<String, Service> reviewStorageMemcached = associateServiceToApp(appName, "review-storage-memcached", 21215, 11211, ServiceTypeEnum.DATABASE,
			Collections.emptySet(), servicesService, dockerHubUsername, null, null);
		servicesMap.put(reviewStorageMemcached.getKey(), reviewStorageMemcached.getValue());
		Map.Entry<String, Service> userReview = associateServiceToApp(appName, "user-review", 10008, 9090, ServiceTypeEnum.BACKEND,
			Collections.emptySet(), servicesService, dockerHubUsername, List.of("user-review-db", "user-review-redis"), null);
		servicesMap.put(userReview.getKey(), userReview.getValue());
		Map.Entry<String, Service> userReviewDb = associateServiceToApp(appName, "user-review-db", 43017, 27017, ServiceTypeEnum.DATABASE,
			Collections.emptySet(), servicesService, dockerHubUsername, null, null);
		servicesMap.put(userReviewDb.getKey(), userReviewDb.getValue());
		Map.Entry<String, Service> userReviewRedis = associateServiceToApp(appName, "user-review-redis", 6381, 6379, ServiceTypeEnum.DATABASE,
			Collections.emptySet(), servicesService, dockerHubUsername, null, null);
		servicesMap.put(userReviewRedis.getKey(), userReviewRedis.getValue());
		Map.Entry<String, Service> movieReview = associateServiceToApp(appName, "movie-review", 10009, 9090, ServiceTypeEnum.BACKEND,
			Collections.emptySet(), servicesService, dockerHubUsername, List.of("movie-review-db", "movie-review-redis"), null);
		servicesMap.put(movieReview.getKey(), movieReview.getValue());
		Map.Entry<String, Service> movieReviewDb = associateServiceToApp(appName, "movie-review-db", 44017, 27017, ServiceTypeEnum.DATABASE,
			Collections.emptySet(), servicesService, dockerHubUsername, null, null);
		servicesMap.put(movieReviewDb.getKey(), movieReviewDb.getValue());
		Map.Entry<String, Service> movieReviewRedis = associateServiceToApp(appName, "movie-review-redis", 6380, 6379, ServiceTypeEnum.DATABASE,
			Collections.emptySet(), servicesService, dockerHubUsername, null, null);
		servicesMap.put(movieReviewRedis.getKey(), movieReviewRedis.getValue());
		/*Volumes
			- ./nginx-web-server/lua-scripts:/usr/local/openresty/nginx/lua-scripts
			- ./nginx-web-server/conf/nginx.conf:/usr/local/openresty/nginx/conf/nginx.conf
			- ./nginx-web-server/jaeger-config.json:/usr/local/openresty/nginx/jaeger-config.json
			- ./gen-lua:/gen-lua*/

		Map.Entry<String, Service> nginxWebServer = associateServiceToApp(appName, "nginx-web-server", 18080, 8080, ServiceTypeEnum.FRONTEND,
			Collections.emptySet(), servicesService, dockerHubUsername, null, null);
		servicesMap.put(nginxWebServer.getKey(), nginxWebServer.getValue());
		Map.Entry<String, Service> castInfo = associateServiceToApp(appName, "cast-info", 10010, 9090, ServiceTypeEnum.BACKEND,
			Collections.emptySet(), servicesService, dockerHubUsername, List.of("cast-info-db", "cast-info-memcached"), null);
		servicesMap.put(castInfo.getKey(), castInfo.getValue());
		Map.Entry<String, Service> castInfoDb = associateServiceToApp(appName, "cast-info-db", 45017, 27017, ServiceTypeEnum.DATABASE,
			Collections.emptySet(), servicesService, dockerHubUsername, null, null);
		servicesMap.put(castInfoDb.getKey(), castInfoDb.getValue());
		Map.Entry<String, Service> castInfoMemcached = associateServiceToApp(appName, "cast-info-memcached", 11219, 11211, ServiceTypeEnum.DATABASE,
			Collections.emptySet(), servicesService, dockerHubUsername, null, null);
		servicesMap.put(castInfoMemcached.getKey(), castInfoMemcached.getValue());
		Map.Entry<String, Service> plot = associateServiceToApp(appName, "plot", 10011, 9090, ServiceTypeEnum.BACKEND,
			Collections.emptySet(), servicesService, dockerHubUsername, List.of("plot-db", "plot-memcached"), null);
		servicesMap.put(plot.getKey(), plot.getValue());
		Map.Entry<String, Service> plotDb = associateServiceToApp(appName, "plot-db", 46017, 27017, ServiceTypeEnum.DATABASE,
			Collections.emptySet(), servicesService, dockerHubUsername, null, null);
		servicesMap.put(plotDb.getKey(), plotDb.getValue());
		Map.Entry<String, Service> plotMemcached = associateServiceToApp(appName, "plot-memcached", 11220, 11211, ServiceTypeEnum.DATABASE,
			Collections.emptySet(), servicesService, dockerHubUsername, null, null);
		servicesMap.put(plotMemcached.getKey(), plotMemcached.getValue());
		Map.Entry<String, Service> movieInfo = associateServiceToApp(appName, "movie-info", 10012, 9090, ServiceTypeEnum.BACKEND,
			Collections.emptySet(), servicesService, dockerHubUsername, List.of("movie-info-db", "movie-info-memcached"), null);
		servicesMap.put(movieInfo.getKey(), movieInfo.getValue());
		Map.Entry<String, Service> movieInfoDb = associateServiceToApp(appName, "movie-info-db", 47017, 27017, ServiceTypeEnum.DATABASE,
			Collections.emptySet(), servicesService, dockerHubUsername, null, null);
		servicesMap.put(movieInfoDb.getKey(), movieInfoDb.getValue());
		Map.Entry<String, Service> movieInfoMemcached = associateServiceToApp(appName, "movie-info-memcached", 11221, 11211, ServiceTypeEnum.BACKEND,
			Collections.emptySet(), servicesService, dockerHubUsername, null, null);
		servicesMap.put(movieInfoMemcached.getKey(), movieInfoMemcached.getValue());
		Map.Entry<String, Service> page = associateServiceToApp(appName, "page", 9090, 9090, ServiceTypeEnum.FRONTEND,
			Collections.emptySet(), servicesService, dockerHubUsername, null, null);
		servicesMap.put(movieInfoMemcached.getKey(), movieInfoMemcached.getValue());

		/*- 5775:5775/udp, 6831:6831/udp, 6832:6832/udp, 5778:5778, 16686:16686, 14268:14268, 9411:9411*/
		/*Map.Entry<String, Service> jaeger = associateServiceToApp(appName, "jaeger", 16686, 16686, ServiceTypeEnum.BACKEND,
			Set.of("COLLECTOR_ZIPKIN_HTTP_PORT=9411"), servicesService, dockerHubUsername, null, null);
		servicesMap.put(jaeger.getKey(), jaeger.getValue());*/

		if (!appsService.hasApp(appName)) {
			App media = App.builder().name(appName)
				.description("This application contains microservices about movies including info, plots, reviews, etc.")
				.build();
			media = appsService.addApp(media);
			appServices.saveAll(List.of(
				AppService.builder().id(new AppServiceKey(media.getId(), uniqueId.getValue().getServiceName())).app(media).service(uniqueId.getValue()).launchOrder(1).build(),
				AppService.builder().id(new AppServiceKey(media.getId(), movieIdDb.getValue().getServiceName())).app(media).service(movieIdDb.getValue()).launchOrder(2).build(),
				AppService.builder().id(new AppServiceKey(media.getId(), movieIdMemcached.getValue().getServiceName())).app(media).service(movieIdMemcached.getValue()).launchOrder(3).build(),
				AppService.builder().id(new AppServiceKey(media.getId(), movieId.getValue().getServiceName())).app(media).service(movieId.getValue()).launchOrder(4).build(),
				AppService.builder().id(new AppServiceKey(media.getId(), text.getValue().getServiceName())).app(media).service(text.getValue()).launchOrder(5).build(),
				AppService.builder().id(new AppServiceKey(media.getId(), ratingRedis.getValue().getServiceName())).app(media).service(ratingRedis.getValue()).launchOrder(6).build(),
				AppService.builder().id(new AppServiceKey(media.getId(), rating.getValue().getServiceName())).app(media).service(rating.getValue()).launchOrder(7).build(),
				AppService.builder().id(new AppServiceKey(media.getId(), userDb.getValue().getServiceName())).app(media).service(userDb.getValue()).launchOrder(8).build(),
				AppService.builder().id(new AppServiceKey(media.getId(), userMemcached.getValue().getServiceName())).app(media).service(userMemcached.getValue()).launchOrder(9).build(),
				AppService.builder().id(new AppServiceKey(media.getId(), user.getValue().getServiceName())).app(media).service(user.getValue()).launchOrder(10).build(),
				AppService.builder().id(new AppServiceKey(media.getId(), composeReviewMemcached.getValue().getServiceName())).app(media).service(composeReviewMemcached.getValue()).launchOrder(11).build(),
				AppService.builder().id(new AppServiceKey(media.getId(), composeReview.getValue().getServiceName())).app(media).service(composeReview.getValue()).launchOrder(12).build(),
				AppService.builder().id(new AppServiceKey(media.getId(), reviewStorageDb.getValue().getServiceName())).app(media).service(reviewStorageDb.getValue()).launchOrder(13).build(),
				AppService.builder().id(new AppServiceKey(media.getId(), reviewStorageMemcached.getValue().getServiceName())).app(media).service(reviewStorageMemcached.getValue()).launchOrder(14).build(),
				AppService.builder().id(new AppServiceKey(media.getId(), reviewStorage.getValue().getServiceName())).app(media).service(reviewStorage.getValue()).launchOrder(15).build(),
				AppService.builder().id(new AppServiceKey(media.getId(), userReviewDb.getValue().getServiceName())).app(media).service(userReviewDb.getValue()).launchOrder(16).build(),
				AppService.builder().id(new AppServiceKey(media.getId(), userReviewRedis.getValue().getServiceName())).app(media).service(userReviewRedis.getValue()).launchOrder(17).build(),
				AppService.builder().id(new AppServiceKey(media.getId(), userReview.getValue().getServiceName())).app(media).service(userReview.getValue()).launchOrder(18).build(),
				AppService.builder().id(new AppServiceKey(media.getId(), movieReviewDb.getValue().getServiceName())).app(media).service(movieReviewDb.getValue()).launchOrder(19).build(),
				AppService.builder().id(new AppServiceKey(media.getId(), movieReviewRedis.getValue().getServiceName())).app(media).service(movieReviewRedis.getValue()).launchOrder(20).build(),
				AppService.builder().id(new AppServiceKey(media.getId(), movieReview.getValue().getServiceName())).app(media).service(movieReview.getValue()).launchOrder(21).build(),
				AppService.builder().id(new AppServiceKey(media.getId(), nginxWebServer.getValue().getServiceName())).app(media).service(nginxWebServer.getValue()).launchOrder(21).build(),
				AppService.builder().id(new AppServiceKey(media.getId(), castInfoDb.getValue().getServiceName())).app(media).service(castInfoDb.getValue()).launchOrder(22).build(),
				AppService.builder().id(new AppServiceKey(media.getId(), castInfoMemcached.getValue().getServiceName())).app(media).service(castInfoMemcached.getValue()).launchOrder(23).build(),
				AppService.builder().id(new AppServiceKey(media.getId(), castInfo.getValue().getServiceName())).app(media).service(castInfo.getValue()).launchOrder(24).build(),
				AppService.builder().id(new AppServiceKey(media.getId(), plotDb.getValue().getServiceName())).app(media).service(plotDb.getValue()).launchOrder(25).build(),
				AppService.builder().id(new AppServiceKey(media.getId(), plotMemcached.getValue().getServiceName())).app(media).service(plotMemcached.getValue()).launchOrder(26).build(),
				AppService.builder().id(new AppServiceKey(media.getId(), plot.getValue().getServiceName())).app(media).service(plot.getValue()).launchOrder(27).build(),
				AppService.builder().id(new AppServiceKey(media.getId(), movieInfoDb.getValue().getServiceName())).app(media).service(movieInfoDb.getValue()).launchOrder(28).build(),
				AppService.builder().id(new AppServiceKey(media.getId(), movieInfoMemcached.getValue().getServiceName())).app(media).service(movieInfoMemcached.getValue()).launchOrder(29).build(),
				AppService.builder().id(new AppServiceKey(media.getId(), movieInfo.getValue().getServiceName())).app(media).service(movieInfo.getValue()).launchOrder(30).build(),
				AppService.builder().id(new AppServiceKey(media.getId(), page.getValue().getServiceName())).app(media).service(page.getValue()).launchOrder(31).build())
				//AppService.builder().id(new AppServiceKey(media.getId(), jaeger.getValue().getServiceName())).app(media).service(jaeger.getValue()).launchOrder(32).build())
			);
		}

		if (!serviceDependenciesService.hasDependency(castInfo.getKey(), castInfoDb.getKey())) {
			serviceDependenciesService.addDependency(castInfo.getValue(), castInfoDb.getValue());
		}
		if (!serviceDependenciesService.hasDependency(castInfo.getKey(), castInfoMemcached.getKey())) {
			serviceDependenciesService.addDependency(castInfo.getValue(), castInfoMemcached.getValue());
		}
		if (!serviceDependenciesService.hasDependency(composeReview.getKey(), reviewStorage.getKey())) {
			serviceDependenciesService.addDependency(composeReview.getValue(), reviewStorage.getValue());
		}
		if (!serviceDependenciesService.hasDependency(composeReview.getKey(), userReview.getKey())) {
			serviceDependenciesService.addDependency(composeReview.getValue(), userReview.getValue());
		}
		if (!serviceDependenciesService.hasDependency(composeReview.getKey(), movieReview.getKey())) {
			serviceDependenciesService.addDependency(composeReview.getValue(), movieReview.getValue());
		}
		if (!serviceDependenciesService.hasDependency(composeReview.getKey(), composeReviewMemcached.getKey())) {
			serviceDependenciesService.addDependency(composeReview.getValue(), composeReviewMemcached.getValue());
		}
		if (!serviceDependenciesService.hasDependency(movieId.getKey(), composeReview.getKey())) {
			serviceDependenciesService.addDependency(movieId.getValue(), composeReview.getValue());
		}
		if (!serviceDependenciesService.hasDependency(movieId.getKey(), rating.getKey())) {
			serviceDependenciesService.addDependency(movieId.getValue(), rating.getValue());
		}
		if (!serviceDependenciesService.hasDependency(movieId.getKey(), movieIdDb.getKey())) {
			serviceDependenciesService.addDependency(movieId.getValue(), movieIdDb.getValue());
		}
		if (!serviceDependenciesService.hasDependency(movieId.getKey(), movieIdMemcached.getKey())) {
			serviceDependenciesService.addDependency(movieId.getValue(), movieIdMemcached.getValue());
		}
		if (!serviceDependenciesService.hasDependency(movieInfo.getKey(), movieInfoDb.getKey())) {
			serviceDependenciesService.addDependency(movieInfo.getValue(), movieInfoDb.getValue());
		}
		if (!serviceDependenciesService.hasDependency(movieInfo.getKey(), movieInfoMemcached.getKey())) {
			serviceDependenciesService.addDependency(movieInfo.getValue(), movieInfoMemcached.getValue());
		}
		if (!serviceDependenciesService.hasDependency(movieReview.getKey(), reviewStorage.getKey())) {
			serviceDependenciesService.addDependency(movieReview.getValue(), reviewStorage.getValue());
		}
		if (!serviceDependenciesService.hasDependency(movieReview.getKey(), movieReviewDb.getKey())) {
			serviceDependenciesService.addDependency(movieReview.getValue(), movieReviewDb.getValue());
		}
		if (!serviceDependenciesService.hasDependency(movieReview.getKey(), movieReviewRedis.getKey())) {
			serviceDependenciesService.addDependency(movieReview.getValue(), movieReviewRedis.getValue());
		}
		if (!serviceDependenciesService.hasDependency(page.getKey(), castInfo.getKey())) {
			serviceDependenciesService.addDependency(page.getValue(), castInfo.getValue());
		}
		if (!serviceDependenciesService.hasDependency(page.getKey(), movieReview.getKey())) {
			serviceDependenciesService.addDependency(page.getValue(), movieReview.getValue());
		}
		if (!serviceDependenciesService.hasDependency(page.getKey(), movieInfo.getKey())) {
			serviceDependenciesService.addDependency(page.getValue(), movieInfo.getValue());
		}
		if (!serviceDependenciesService.hasDependency(page.getKey(), plot.getKey())) {
			serviceDependenciesService.addDependency(page.getValue(), plot.getValue());
		}
		if (!serviceDependenciesService.hasDependency(plot.getKey(), plotDb.getKey())) {
			serviceDependenciesService.addDependency(page.getValue(), plotDb.getValue());
		}
		if (!serviceDependenciesService.hasDependency(plot.getKey(), plotMemcached.getKey())) {
			serviceDependenciesService.addDependency(plot.getValue(), plotMemcached.getValue());
		}
		if (!serviceDependenciesService.hasDependency(rating.getKey(), composeReview.getKey())) {
			serviceDependenciesService.addDependency(rating.getValue(), composeReview.getValue());
		}
		if (!serviceDependenciesService.hasDependency(rating.getKey(), ratingRedis.getKey())) {
			serviceDependenciesService.addDependency(rating.getValue(), ratingRedis.getValue());
		}
		if (!serviceDependenciesService.hasDependency(reviewStorage.getKey(), reviewStorageDb.getKey())) {
			serviceDependenciesService.addDependency(reviewStorage.getValue(), reviewStorageDb.getValue());
		}
		if (!serviceDependenciesService.hasDependency(reviewStorage.getKey(), reviewStorageMemcached.getKey())) {
			serviceDependenciesService.addDependency(reviewStorage.getValue(), reviewStorageMemcached.getValue());
		}
		if (!serviceDependenciesService.hasDependency(text.getKey(), composeReview.getKey())) {
			serviceDependenciesService.addDependency(text.getValue(), composeReview.getValue());
		}
		if (!serviceDependenciesService.hasDependency(uniqueId.getKey(), composeReview.getKey())) {
			serviceDependenciesService.addDependency(uniqueId.getValue(), composeReview.getValue());
		}
		if (!serviceDependenciesService.hasDependency(userReview.getKey(), reviewStorage.getKey())) {
			serviceDependenciesService.addDependency(userReview.getValue(), reviewStorage.getValue());
		}
		if (!serviceDependenciesService.hasDependency(userReview.getKey(), userReviewDb.getKey())) {
			serviceDependenciesService.addDependency(userReview.getValue(), userReviewDb.getValue());
		}
		if (!serviceDependenciesService.hasDependency(userReview.getKey(), userReviewRedis.getKey())) {
			serviceDependenciesService.addDependency(userReview.getValue(), userReviewRedis.getValue());
		}
		if (!serviceDependenciesService.hasDependency(user.getKey(), composeReview.getKey())) {
			serviceDependenciesService.addDependency(user.getValue(), composeReview.getValue());
		}
		if (!serviceDependenciesService.hasDependency(user.getKey(), userDb.getKey())) {
			serviceDependenciesService.addDependency(user.getValue(), userDb.getValue());
		}
		if (!serviceDependenciesService.hasDependency(user.getKey(), userMemcached.getKey())) {
			serviceDependenciesService.addDependency(user.getValue(), userMemcached.getValue());
		}

		return servicesMap;
	}

	@Transactional
	Map<String, Service> loadHotelReservation(String dockerHubUsername, AppsService appsService, ServicesService servicesService,
											  ServiceDependenciesService serviceDependenciesService, AppServices appServices) {
		Map<String, Service> servicesMap = new HashMap<>(17);

		String appName = "Hotel Reservation";

		Map.Entry<String, Service> frontend = associateServiceToApp(appName, "frontend", 5000, 5000, ServiceTypeEnum.FRONTEND,
			Collections.emptySet(), servicesService, dockerHubUsername, null, null);
		servicesMap.put(frontend.getKey(), frontend.getValue());
		Map.Entry<String, Service> profile = associateServiceToApp(appName, "profile", 6555, 8081, ServiceTypeEnum.BACKEND,
			Collections.emptySet(), servicesService, dockerHubUsername, List.of("profile-db", "profile-memcached"), null);
		servicesMap.put(profile.getKey(), profile.getValue());
		// volume - profile:/data/db
		Map.Entry<String, Service> profileDb = associateServiceToApp(appName, "profile-db", 29017, 27017, ServiceTypeEnum.DATABASE,
			Collections.emptySet(), servicesService, dockerHubUsername, null, null);
		servicesMap.put(profileDb.getKey(), profileDb.getValue());
		Map.Entry<String, Service> memcachedProfile = associateServiceToApp(appName, "profile-memcached", 11213, 11211, ServiceTypeEnum.DATABASE,
			Set.of("MEMCACHED_CACHE_SIZE=128", "MEMCACHED_THREADS=2"), servicesService, dockerHubUsername, null, null);
		servicesMap.put(memcachedProfile.getKey(), memcachedProfile.getValue());
		Map.Entry<String, Service> search = associateServiceToApp(appName, "search", 6677, 8082, ServiceTypeEnum.BACKEND,
			Collections.emptySet(), servicesService, dockerHubUsername, null, null);
		servicesMap.put(search.getKey(), search.getValue());
		Map.Entry<String, Service> geo = associateServiceToApp(appName, "geo", 6777, 8083, ServiceTypeEnum.BACKEND,
			Collections.emptySet(), servicesService, dockerHubUsername, List.of("geo-db"), null);
		servicesMap.put(geo.getKey(), geo.getValue());
		// volume - geo:/data/db
		Map.Entry<String, Service> geoDb = associateServiceToApp(appName, "geo-db", 28017, 27017, ServiceTypeEnum.DATABASE,
			Collections.emptySet(), servicesService, dockerHubUsername, null, null);
		servicesMap.put(geoDb.getKey(), geoDb.getValue());
		Map.Entry<String, Service> rate = associateServiceToApp(appName, "rate", 6888, 8084, ServiceTypeEnum.BACKEND,
			Collections.emptySet(), servicesService, dockerHubUsername, List.of("date-db", "rate-memcached"), null);
		servicesMap.put(rate.getKey(), rate.getValue());
		// volume - rate:/data/db
		Map.Entry<String, Service> rateDb = associateServiceToApp(appName, "rate-db", 30017, 27017, ServiceTypeEnum.DATABASE,
			Collections.emptySet(), servicesService, dockerHubUsername, null, null);
		servicesMap.put(rateDb.getKey(), rateDb.getValue());
		Map.Entry<String, Service> memcachedRate = associateServiceToApp(appName, "rate-memcached", 11212, 11211, ServiceTypeEnum.DATABASE,
			Set.of("MEMCACHED_CACHE_SIZE=128", "MEMCACHED_THREADS=2"), servicesService, dockerHubUsername, null, null);
		servicesMap.put(memcachedRate.getKey(), memcachedRate.getValue());
		Map.Entry<String, Service> recommendation = associateServiceToApp(appName, "recommendation", 6999, 8085, ServiceTypeEnum.BACKEND,
			Collections.emptySet(), servicesService, dockerHubUsername, List.of("recommendation-db"), null);
		servicesMap.put(recommendation.getKey(), recommendation.getValue());
		// volume - recommendation:/data/db
		Map.Entry<String, Service> recommendationDb = associateServiceToApp(appName, "recommendation-db", 31017, 27017, ServiceTypeEnum.DATABASE,
			Collections.emptySet(), servicesService, dockerHubUsername, null, null);
		servicesMap.put(recommendationDb.getKey(), recommendationDb.getValue());
		Map.Entry<String, Service> user = associateServiceToApp(appName, "user", 7111, 8086, ServiceTypeEnum.BACKEND,
			Collections.emptySet(), servicesService, dockerHubUsername, List.of("user-db"), null);
		servicesMap.put(user.getKey(), user.getValue());
		// volume - user:/data/db
		Map.Entry<String, Service> userDb = associateServiceToApp(appName, "user-db", 33017, 27017, ServiceTypeEnum.DATABASE,
			Collections.emptySet(), servicesService, dockerHubUsername, null, null);
		servicesMap.put(userDb.getKey(), userDb.getValue());
		Map.Entry<String, Service> reservation = associateServiceToApp(appName, "reservation", 7222, 8087, ServiceTypeEnum.BACKEND,
			Collections.emptySet(), servicesService, dockerHubUsername, List.of("reservation-db", "reservation-memcached"), null);
		servicesMap.put(reservation.getKey(), reservation.getValue());
		// volume - reservation:/data/db
		Map.Entry<String, Service> reservationDb = associateServiceToApp(appName, "reservation-db", 32017, 27017, ServiceTypeEnum.DATABASE,
			Collections.emptySet(), servicesService, dockerHubUsername, null, null);
		servicesMap.put(reservationDb.getKey(), reservationDb.getValue());
		Map.Entry<String, Service> memcachedReserve = associateServiceToApp(appName, "reservation-memcached", 11214, 11211, ServiceTypeEnum.DATABASE,
			Set.of("MEMCACHED_CACHE_SIZE=128", "MEMCACHED_THREADS=2"), servicesService, dockerHubUsername, null, null);
		servicesMap.put(memcachedReserve.getKey(), memcachedReserve.getValue());
		// available: "14269", "5778:5778", "14268:14268", "14267", "16686:16686", "5775:5775/udp", "6831:6831/udp", "6832:6832/udp"
		/*Map.Entry<String, Service> jaeger = associateServiceToApp(appName, "jaeger", 5778, 5778, ServiceTypeEnum.BACKEND,
			Collections.emptySet(), servicesService, dockerHubUsername, null, null);
		servicesMap.put(reservation.getKey(), reservation.getValue());*/

		if (!appsService.hasApp(appName)) {
			App hotelReservation = App.builder().name(appName)
				.description("The application implements a hotel reservation service, build with Go and gRPC, and " +
					"starting from the open-source project https://github.com/harlow/go-micro-services. The initial " +
					"project is extended in several ways, including adding back-end in-memory and persistent databases, " +
					"adding a recommender system for obtaining hotel recommendations, and adding the functionality to" +
					" place a hotel reservation")
				.build();
			hotelReservation = appsService.addApp(hotelReservation);
			appServices.saveAll(List.of(
				//AppService.builder().id(new AppServiceKey(hotelReservation.getId(), jaeger.getValue().getServiceName())).app(hotelReservation).service(jaeger.getValue()).launchOrder(1).build(),
				AppService.builder().id(new AppServiceKey(hotelReservation.getId(), memcachedProfile.getValue().getServiceName())).app(hotelReservation).service(memcachedProfile.getValue()).launchOrder(2).build(),
				AppService.builder().id(new AppServiceKey(hotelReservation.getId(), profileDb.getValue().getServiceName())).app(hotelReservation).service(profileDb.getValue()).launchOrder(3).build(),
				AppService.builder().id(new AppServiceKey(hotelReservation.getId(), profile.getValue().getServiceName())).app(hotelReservation).service(profile.getValue()).launchOrder(4).build(),
				AppService.builder().id(new AppServiceKey(hotelReservation.getId(), search.getValue().getServiceName())).app(hotelReservation).service(search.getValue()).launchOrder(5).build(),
				AppService.builder().id(new AppServiceKey(hotelReservation.getId(), geoDb.getValue().getServiceName())).app(hotelReservation).service(geoDb.getValue()).launchOrder(6).build(),
				AppService.builder().id(new AppServiceKey(hotelReservation.getId(), geo.getValue().getServiceName())).app(hotelReservation).service(geo.getValue()).launchOrder(7).build(),
				AppService.builder().id(new AppServiceKey(hotelReservation.getId(), rateDb.getValue().getServiceName())).app(hotelReservation).service(rateDb.getValue()).launchOrder(8).build(),
				AppService.builder().id(new AppServiceKey(hotelReservation.getId(), memcachedRate.getValue().getServiceName())).app(hotelReservation).service(memcachedRate.getValue()).launchOrder(9).build(),
				AppService.builder().id(new AppServiceKey(hotelReservation.getId(), rate.getValue().getServiceName())).app(hotelReservation).service(rate.getValue()).launchOrder(10).build(),
				AppService.builder().id(new AppServiceKey(hotelReservation.getId(), recommendationDb.getValue().getServiceName())).app(hotelReservation).service(recommendationDb.getValue()).launchOrder(12).build(),
				AppService.builder().id(new AppServiceKey(hotelReservation.getId(), recommendation.getValue().getServiceName())).app(hotelReservation).service(recommendation.getValue()).launchOrder(13).build(),
				AppService.builder().id(new AppServiceKey(hotelReservation.getId(), userDb.getValue().getServiceName())).app(hotelReservation).service(userDb.getValue()).launchOrder(14).build(),
				AppService.builder().id(new AppServiceKey(hotelReservation.getId(), user.getValue().getServiceName())).app(hotelReservation).service(user.getValue()).launchOrder(15).build(),
				AppService.builder().id(new AppServiceKey(hotelReservation.getId(), reservationDb.getValue().getServiceName())).app(hotelReservation).service(reservationDb.getValue()).launchOrder(16).build(),
				AppService.builder().id(new AppServiceKey(hotelReservation.getId(), memcachedReserve.getValue().getServiceName())).app(hotelReservation).service(memcachedReserve.getValue()).launchOrder(17).build(),
				AppService.builder().id(new AppServiceKey(hotelReservation.getId(), reservation.getValue().getServiceName())).app(hotelReservation).service(reservation.getValue()).launchOrder(18).build(),
				AppService.builder().id(new AppServiceKey(hotelReservation.getId(), frontend.getValue().getServiceName())).app(hotelReservation).service(frontend.getValue()).launchOrder(19).build())
			);
		}

		if (!serviceDependenciesService.hasDependency(profile.getKey(), profileDb.getKey())) {
			serviceDependenciesService.addDependency(profile.getValue(), profileDb.getValue());
		}
		if (!serviceDependenciesService.hasDependency(profile.getKey(), memcachedProfile.getKey())) {
			serviceDependenciesService.addDependency(profile.getValue(), memcachedProfile.getValue());
		}
		if (!serviceDependenciesService.hasDependency(geo.getKey(), geoDb.getKey())) {
			serviceDependenciesService.addDependency(geo.getValue(), geoDb.getValue());
		}
		if (!serviceDependenciesService.hasDependency(rate.getKey(), rateDb.getKey())) {
			serviceDependenciesService.addDependency(rate.getValue(), rateDb.getValue());
		}
		if (!serviceDependenciesService.hasDependency(rate.getKey(), memcachedRate.getKey())) {
			serviceDependenciesService.addDependency(rate.getValue(), memcachedRate.getValue());
		}
		if (!serviceDependenciesService.hasDependency(recommendation.getKey(), recommendationDb.getKey())) {
			serviceDependenciesService.addDependency(recommendation.getValue(), recommendationDb.getValue());
		}
		if (!serviceDependenciesService.hasDependency(user.getKey(), userDb.getKey())) {
			serviceDependenciesService.addDependency(user.getValue(), userDb.getValue());
		}
		if (!serviceDependenciesService.hasDependency(reservation.getKey(), reservationDb.getKey())) {
			serviceDependenciesService.addDependency(reservation.getValue(), reservationDb.getValue());
		}
		if (!serviceDependenciesService.hasDependency(reservation.getKey(), memcachedReserve.getKey())) {
			serviceDependenciesService.addDependency(reservation.getValue(), memcachedReserve.getValue());
		}
		if (!serviceDependenciesService.hasDependency(frontend.getKey(), profile.getKey())) {
			serviceDependenciesService.addDependency(frontend.getValue(), profile.getValue());
		}
		if (!serviceDependenciesService.hasDependency(frontend.getKey(), rate.getKey())) {
			serviceDependenciesService.addDependency(frontend.getValue(), rate.getValue());
		}
		if (!serviceDependenciesService.hasDependency(frontend.getKey(), recommendation.getKey())) {
			serviceDependenciesService.addDependency(frontend.getValue(), recommendation.getValue());
		}
		if (!serviceDependenciesService.hasDependency(frontend.getKey(), reservation.getKey())) {
			serviceDependenciesService.addDependency(frontend.getValue(), reservation.getValue());
		}
		if (!serviceDependenciesService.hasDependency(frontend.getKey(), user.getKey())) {
			serviceDependenciesService.addDependency(frontend.getValue(), user.getValue());
		}
		if (!serviceDependenciesService.hasDependency(frontend.getKey(), search.getKey())) {
			serviceDependenciesService.addDependency(frontend.getValue(), search.getValue());
		}
		if (!serviceDependenciesService.hasDependency(search.getKey(), geo.getKey())) {
			serviceDependenciesService.addDependency(search.getValue(), geo.getValue());
		}
		if (!serviceDependenciesService.hasDependency(search.getKey(), rate.getKey())) {
			serviceDependenciesService.addDependency(search.getValue(), rate.getValue());
		}

		return servicesMap;
	}

	@Transactional
	Map<String, Service> loadOnlineBoutique(String dockerHubUsername, AppsService appsService, ServicesService servicesService,
											ServiceDependenciesService serviceDependenciesService, AppServices appServices) {
		Map<String, Service> servicesMap = new HashMap<>(12);

		String appName = "Online Boutique";

		Map.Entry<String, Service> ads = associateServiceToApp(appName, "ads", 9555, 9555, ServiceTypeEnum.BACKEND,
			Collections.emptySet(), servicesService, dockerHubUsername, null, 180000000d);
		servicesMap.put(ads.getKey(), ads.getValue());
		Map.Entry<String, Service> carts = associateServiceToApp(appName, "carts", 7070, 7070, ServiceTypeEnum.BACKEND,
			Collections.emptySet(), servicesService, dockerHubUsername, List.of("carts-redis"), 64000000d);
		servicesMap.put(carts.getKey(), carts.getValue());
		Map.Entry<String, Service> cartsRedis = associateServiceToApp(appName, "carts-redis", 6379, 6379, ServiceTypeEnum.DATABASE,
			Collections.emptySet(), servicesService, dockerHubUsername, null, 200000000d);
		servicesMap.put(cartsRedis.getKey(), cartsRedis.getValue());
		Map.Entry<String, Service> checkout = associateServiceToApp(appName, "checkout", 5050, 5050, ServiceTypeEnum.BACKEND,
			Collections.emptySet(), servicesService, dockerHubUsername, null, 64000000d);
		servicesMap.put(checkout.getKey(), checkout.getValue());
		Map.Entry<String, Service> currency = associateServiceToApp(appName, "currency", 7000, 7000, ServiceTypeEnum.BACKEND,
			Collections.emptySet(), servicesService, dockerHubUsername, null, 64000000d);
		servicesMap.put(currency.getKey(), currency.getValue());
		Map.Entry<String, Service> email = associateServiceToApp(appName, "email", 8000, 8080, ServiceTypeEnum.BACKEND,
			Set.of("DISABLE_PROFILER=1"), servicesService, dockerHubUsername, null, 64000000d);
		servicesMap.put(email.getKey(), email.getValue());
		Map.Entry<String, Service> frontend = associateServiceToApp(appName, "frontend", 8090, 8080, ServiceTypeEnum.FRONTEND,
			Set.of("ENV_PLATFORM=gcp"), servicesService, dockerHubUsername, null, 64000000d);
		servicesMap.put(frontend.getKey(), frontend.getValue());
		Map.Entry<String, Service> loadGenerator = associateServiceToApp(appName, "loadGenerator", null, null, ServiceTypeEnum.BACKEND,
			Set.of("USERS=10"), servicesService, dockerHubUsername, null, 256000000d);
		servicesMap.put(loadGenerator.getKey(), loadGenerator.getValue());
		Map.Entry<String, Service> payment = associateServiceToApp(appName, "payment", 50051, 50051, ServiceTypeEnum.BACKEND,
			Collections.emptySet(), servicesService, dockerHubUsername, null, 64000000d);
		servicesMap.put(payment.getKey(), payment.getValue());
		Map.Entry<String, Service> catalogue = associateServiceToApp(appName, "catalogue", 3550, 3550, ServiceTypeEnum.BACKEND,
			Collections.emptySet(), servicesService, dockerHubUsername, null, 64000000d);
		servicesMap.put(catalogue.getKey(), catalogue.getValue());
		Map.Entry<String, Service> recommendation = associateServiceToApp(appName, "recommendation", 5080, 8080, ServiceTypeEnum.BACKEND,
			Collections.emptySet(), servicesService, dockerHubUsername, null, 220000000d);
		servicesMap.put(recommendation.getKey(), recommendation.getValue());
		Map.Entry<String, Service> shipping = associateServiceToApp(appName, "shipping", 50051, 50051, ServiceTypeEnum.BACKEND,
			Collections.emptySet(), servicesService, dockerHubUsername, null, 64000000d);
		servicesMap.put(shipping.getKey(), shipping.getValue());

		if (!appsService.hasApp(appName)) {
			App onlineBoutique = App.builder()
				.name(appName)
				.description("Online Boutique is a cloud-native microservices demo application. " +
					"Online Boutique consists of a 10-tier microservices application. The application is a web-based " +
					"e-commerce app where users can browse items, add them to the cart, and purchase them.")
				.build();
			onlineBoutique = appsService.addApp(onlineBoutique);
			appServices.saveAll(List.of(
				AppService.builder().id(new AppServiceKey(onlineBoutique.getId(), payment.getValue().getServiceName())).app(onlineBoutique).service(payment.getValue()).launchOrder(1).build(),
				AppService.builder().id(new AppServiceKey(onlineBoutique.getId(), email.getValue().getServiceName())).app(onlineBoutique).service(email.getValue()).launchOrder(2).build(),
				AppService.builder().id(new AppServiceKey(onlineBoutique.getId(), carts.getValue().getServiceName())).app(onlineBoutique).service(carts.getValue()).launchOrder(3).build(),
				AppService.builder().id(new AppServiceKey(onlineBoutique.getId(), recommendation.getValue().getServiceName())).app(onlineBoutique).service(recommendation.getValue()).launchOrder(4).build(),
				AppService.builder().id(new AppServiceKey(onlineBoutique.getId(), catalogue.getValue().getServiceName())).app(onlineBoutique).service(catalogue.getValue()).launchOrder(5).build(),
				AppService.builder().id(new AppServiceKey(onlineBoutique.getId(), currency.getValue().getServiceName())).app(onlineBoutique).service(currency.getValue()).launchOrder(6).build(),
				AppService.builder().id(new AppServiceKey(onlineBoutique.getId(), shipping.getValue().getServiceName())).app(onlineBoutique).service(shipping.getValue()).launchOrder(7).build(),
				AppService.builder().id(new AppServiceKey(onlineBoutique.getId(), checkout.getValue().getServiceName())).app(onlineBoutique).service(checkout.getValue()).launchOrder(8).build(),
				AppService.builder().id(new AppServiceKey(onlineBoutique.getId(), ads.getValue().getServiceName())).app(onlineBoutique).service(ads.getValue()).launchOrder(9).build(),
				AppService.builder().id(new AppServiceKey(onlineBoutique.getId(), frontend.getValue().getServiceName())).app(onlineBoutique).service(frontend.getValue()).launchOrder(10).build(),
				AppService.builder().id(new AppServiceKey(onlineBoutique.getId(), loadGenerator.getValue().getServiceName())).app(onlineBoutique).service(loadGenerator.getValue()).launchOrder(11).build()));
		}

		if (!serviceDependenciesService.hasDependency(carts.getKey(), cartsRedis.getKey())) {
			serviceDependenciesService.addDependency(carts.getValue(), cartsRedis.getValue());
		}
		if (!serviceDependenciesService.hasDependency(checkout.getKey(), catalogue.getKey())) {
			serviceDependenciesService.addDependency(checkout.getValue(), catalogue.getValue());
		}
		if (!serviceDependenciesService.hasDependency(checkout.getKey(), shipping.getKey())) {
			serviceDependenciesService.addDependency(checkout.getValue(), shipping.getValue());
		}
		if (!serviceDependenciesService.hasDependency(checkout.getKey(), payment.getKey())) {
			serviceDependenciesService.addDependency(checkout.getValue(), payment.getValue());
		}
		if (!serviceDependenciesService.hasDependency(checkout.getKey(), email.getKey())) {
			serviceDependenciesService.addDependency(checkout.getValue(), email.getValue());
		}
		if (!serviceDependenciesService.hasDependency(checkout.getKey(), currency.getKey())) {
			serviceDependenciesService.addDependency(checkout.getValue(), currency.getValue());
		}
		if (!serviceDependenciesService.hasDependency(checkout.getKey(), carts.getKey())) {
			serviceDependenciesService.addDependency(checkout.getValue(), carts.getValue());
		}
		if (!serviceDependenciesService.hasDependency(recommendation.getKey(), catalogue.getKey())) {
			serviceDependenciesService.addDependency(recommendation.getValue(), catalogue.getValue());
		}
		if (!serviceDependenciesService.hasDependency(loadGenerator.getKey(), frontend.getKey())) {
			serviceDependenciesService.addDependency(loadGenerator.getValue(), frontend.getValue());
		}
		if (!serviceDependenciesService.hasDependency(frontend.getKey(), catalogue.getKey())) {
			serviceDependenciesService.addDependency(frontend.getValue(), catalogue.getValue());
		}
		if (!serviceDependenciesService.hasDependency(frontend.getKey(), currency.getKey())) {
			serviceDependenciesService.addDependency(frontend.getValue(), currency.getValue());
		}
		if (!serviceDependenciesService.hasDependency(frontend.getKey(), carts.getKey())) {
			serviceDependenciesService.addDependency(frontend.getValue(), carts.getValue());
		}
		if (!serviceDependenciesService.hasDependency(frontend.getKey(), recommendation.getKey())) {
			serviceDependenciesService.addDependency(frontend.getValue(), recommendation.getValue());
		}
		if (!serviceDependenciesService.hasDependency(frontend.getKey(), shipping.getKey())) {
			serviceDependenciesService.addDependency(frontend.getValue(), shipping.getValue());
		}
		if (!serviceDependenciesService.hasDependency(frontend.getKey(), checkout.getKey())) {
			serviceDependenciesService.addDependency(frontend.getValue(), checkout.getValue());
		}
		if (!serviceDependenciesService.hasDependency(frontend.getKey(), ads.getKey())) {
			serviceDependenciesService.addDependency(frontend.getValue(), ads.getValue());
		}

		return servicesMap;
	}

	/*@Transactional
	Map<String, Service> loadMixal(String dockerHubUsername, AppsService appsService, ServicesService servicesService,
								   ServiceDependenciesService serviceDependenciesService, AppServices appServices) {
		Map<String, Service> servicesMap = new HashMap<>(5);

		String appName = "Mixal";

		Map.Entry<String, Service> prime = associateServiceToApp(appName, "prime", 9001, 80, ServiceTypeEnum.BACKEND,
			Collections.emptySet(), servicesService, dockerHubUsername, null, null);
		servicesMap.put(prime.getKey(), prime.getValue());
		Map.Entry<String, Service> movie = associateServiceToApp(appName, "movie", 9002, 80, ServiceTypeEnum.BACKEND,
			Collections.emptySet(), servicesService, dockerHubUsername, List.of("movie-db"), null);
		servicesMap.put(movie.getKey(), movie.getValue());
		Map.Entry<String, Service> movieDb = associateServiceToApp(appName, "movie-db", 22027, 28027, ServiceTypeEnum.DATABASE,
			Collections.emptySet(), servicesService, dockerHubUsername, null, null);
		servicesMap.put(movieDb.getKey(), movieDb.getValue());
		Map.Entry<String, Service> serve = associateServiceToApp(appName, "serve", 9003, 80, ServiceTypeEnum.BACKEND,
			Collections.emptySet(), servicesService, dockerHubUsername, null, null);
		servicesMap.put(serve.getKey(), serve.getValue());
		Map.Entry<String, Service> webac = associateServiceToApp(appName, "webac", 9004, 80, ServiceTypeEnum.FRONTEND,
			Collections.emptySet(), servicesService, dockerHubUsername, null, null);
		servicesMap.put(webac.getKey(), webac.getValue());

		if (!appsService.hasApp(appName)) {
			App mixal = App.builder()
				.name(appName)
				.description("Set of four simple microservices.")
				.build();
			mixal = appsService.addApp(mixal);
			appServices.saveAll(List.of(
				AppService.builder().id(new AppServiceKey(mixal.getId(), movieDb.getValue().getServiceName())).app(mixal).service(movieDb.getValue()).launchOrder(1).build(),
				AppService.builder().id(new AppServiceKey(mixal.getId(), movie.getValue().getServiceName())).app(mixal).service(movie.getValue()).launchOrder(2).build(),
				AppService.builder().id(new AppServiceKey(mixal.getId(), prime.getValue().getServiceName())).app(mixal).service(prime.getValue()).launchOrder(3).build(),
				AppService.builder().id(new AppServiceKey(mixal.getId(), serve.getValue().getServiceName())).app(mixal).service(serve.getValue()).launchOrder(4).build(),
				AppService.builder().id(new AppServiceKey(mixal.getId(), webac.getValue().getServiceName())).app(mixal).service(webac.getValue()).launchOrder(5).build()));
		}
		if (!serviceDependenciesService.hasDependency(serve.getKey(), movie.getKey())) {
			serviceDependenciesService.addDependency(serve.getValue(), movie.getValue());
		}
		if (!serviceDependenciesService.hasDependency(serve.getKey(), prime.getKey())) {
			serviceDependenciesService.addDependency(serve.getValue(), prime.getValue());
		}
		if (!serviceDependenciesService.hasDependency(serve.getKey(), webac.getKey())) {
			serviceDependenciesService.addDependency(serve.getValue(), webac.getValue());
		}
		if (!serviceDependenciesService.hasDependency(serve.getKey(), movieDb.getKey())) {
			serviceDependenciesService.addDependency(serve.getValue(), movieDb.getValue());
		}

		return servicesMap;
	}*/

	@Transactional
	Map<String, Service> loadSockShop(String dockerHubUsername, AppsService appsService, ServicesService servicesService,
									  ServiceDependenciesService serviceDependenciesService, AppServices appServices) {
		Map<String, Service> servicesMap = new HashMap<>(14);

		String appName = "Sock Shop";

		Map.Entry<String, Service> frontend = associateServiceToApp(appName, "frontend", 8079, 8079, ServiceTypeEnum.FRONTEND,
			Collections.emptySet(), servicesService, dockerHubUsername, null, 209715200d);
		servicesMap.put(frontend.getKey(), frontend.getValue());
		Map.Entry<String, Service> user = associateServiceToApp(appName, "user", 8086, 80, ServiceTypeEnum.BACKEND,
			Collections.emptySet(), servicesService, dockerHubUsername, List.of("user-db"), 62914560d);
		servicesMap.put(user.getKey(), user.getValue());
		Map.Entry<String, Service> userDb = associateServiceToApp(appName, "user-db", 15017, 27017, ServiceTypeEnum.DATABASE,
			Collections.emptySet(), servicesService, dockerHubUsername, null, 262144000d);
		servicesMap.put(userDb.getKey(), userDb.getValue());
		Map.Entry<String, Service> catalogue = associateServiceToApp(appName, "catalogue", 8083, 80, ServiceTypeEnum.BACKEND,
			Collections.emptySet(), servicesService, dockerHubUsername, List.of("catalogue-db"), 62914560d);
		servicesMap.put(catalogue.getKey(), catalogue.getValue());
		Map.Entry<String, Service> catalogueDb = associateServiceToApp(appName, "catalogue-db", 3306, 3306, ServiceTypeEnum.DATABASE,
			Collections.emptySet(), servicesService, dockerHubUsername, null, 262144000d);
		servicesMap.put(catalogueDb.getKey(), catalogueDb.getValue());
		Map.Entry<String, Service> payment = associateServiceToApp(appName, "payment", 8084, 80, ServiceTypeEnum.BACKEND,
			Collections.emptySet(), servicesService, dockerHubUsername, null, 62914560d);
		servicesMap.put(payment.getKey(), payment.getValue());
		Map.Entry<String, Service> carts = associateServiceToApp(appName, "carts", 8085, 80, ServiceTypeEnum.BACKEND,
			Collections.emptySet(), servicesService, dockerHubUsername, List.of("carts-db"), 262144000d);
		servicesMap.put(carts.getKey(), carts.getValue());
		Map.Entry<String, Service> cartsDb = associateServiceToApp(appName, "carts-db", 27071, 27017, ServiceTypeEnum.DATABASE,
			Collections.emptySet(), servicesService, dockerHubUsername, null, 262144000d);
		servicesMap.put(cartsDb.getKey(), cartsDb.getValue());
		Map.Entry<String, Service> orders = associateServiceToApp(appName, "orders", 8086, 80, ServiceTypeEnum.BACKEND,
			Collections.emptySet(), servicesService, dockerHubUsername, List.of("orders-db"), 262144000d);
		servicesMap.put(orders.getKey(), orders.getValue());
		Map.Entry<String, Service> ordersDb = associateServiceToApp(appName, "orders-db", 34017, 27017, ServiceTypeEnum.DATABASE,
			Collections.emptySet(), servicesService, dockerHubUsername, null, 262144000d);
		servicesMap.put(ordersDb.getKey(), ordersDb.getValue());
		Map.Entry<String, Service> shipping = associateServiceToApp(appName, "shipping", 8087, 80, ServiceTypeEnum.BACKEND,
			Collections.emptySet(), servicesService, dockerHubUsername, List.of("rabbitmq"), 262144000d);
		servicesMap.put(shipping.getKey(), shipping.getValue());
		Map.Entry<String, Service> queueMaster = associateServiceToApp(appName, "queue-master", 8088, 80, ServiceTypeEnum.BACKEND,
			Collections.emptySet(), servicesService, dockerHubUsername, List.of("rabbitmq"), 262144000d);
		servicesMap.put(queueMaster.getKey(), queueMaster.getValue());
		Map.Entry<String, Service> rabbitmq = associateServiceToApp(appName, "rabbitmq", 5672, 5672, ServiceTypeEnum.BACKEND,
			Collections.emptySet(), servicesService, dockerHubUsername, null, 262144000d);
		servicesMap.put(rabbitmq.getKey(), rabbitmq.getValue());

		if (!appsService.hasApp(appName)) {
			App sockShop = App.builder()
				.name(appName)
				.description("Sock Shop simulates the user-facing part of an e-commerce website that sells socks. " +
					"It is intended to aid the demonstration and testing of microservice and cloud native technologies.")
				.build();
			sockShop = appsService.addApp(sockShop);
			log.info("{}", sockShop.getId());
			appServices.saveAll(List.of(
				AppService.builder().id(new AppServiceKey(sockShop.getId(), rabbitmq.getValue().getServiceName())).app(sockShop).service(rabbitmq.getValue()).launchOrder(1).build(),
				AppService.builder().id(new AppServiceKey(sockShop.getId(), queueMaster.getValue().getServiceName())).app(sockShop).service(queueMaster.getValue()).launchOrder(2).build(),
				AppService.builder().id(new AppServiceKey(sockShop.getId(), shipping.getValue().getServiceName())).app(sockShop).service(shipping.getValue()).launchOrder(3).build(),
				AppService.builder().id(new AppServiceKey(sockShop.getId(), ordersDb.getValue().getServiceName())).app(sockShop).service(ordersDb.getValue()).launchOrder(4).build(),
				AppService.builder().id(new AppServiceKey(sockShop.getId(), orders.getValue().getServiceName())).app(sockShop).service(orders.getValue()).launchOrder(5).build(),
				AppService.builder().id(new AppServiceKey(sockShop.getId(), cartsDb.getValue().getServiceName())).app(sockShop).service(cartsDb.getValue()).launchOrder(6).build(),
				AppService.builder().id(new AppServiceKey(sockShop.getId(), carts.getValue().getServiceName())).app(sockShop).service(carts.getValue()).launchOrder(7).build(),
				AppService.builder().id(new AppServiceKey(sockShop.getId(), payment.getValue().getServiceName())).app(sockShop).service(payment.getValue()).launchOrder(8).build(),
				AppService.builder().id(new AppServiceKey(sockShop.getId(), catalogueDb.getValue().getServiceName())).app(sockShop).service(catalogueDb.getValue()).launchOrder(9).build(),
				AppService.builder().id(new AppServiceKey(sockShop.getId(), catalogue.getValue().getServiceName())).app(sockShop).service(catalogue.getValue()).launchOrder(10).build(),
				AppService.builder().id(new AppServiceKey(sockShop.getId(), userDb.getValue().getServiceName())).app(sockShop).service(userDb.getValue()).launchOrder(11).build(),
				AppService.builder().id(new AppServiceKey(sockShop.getId(), user.getValue().getServiceName())).app(sockShop).service(user.getValue()).launchOrder(12).build(),
				AppService.builder().id(new AppServiceKey(sockShop.getId(), frontend.getValue().getServiceName())).app(sockShop).service(frontend.getValue()).launchOrder(13).build()));
		}

		if (!serviceDependenciesService.hasDependency(frontend.getKey(), user.getKey())) {
			serviceDependenciesService.addDependency(frontend.getValue(), user.getValue());
		}
		if (!serviceDependenciesService.hasDependency(frontend.getKey(), catalogue.getKey())) {
			serviceDependenciesService.addDependency(frontend.getValue(), catalogue.getValue());
		}
		if (!serviceDependenciesService.hasDependency(frontend.getKey(), payment.getKey())) {
			serviceDependenciesService.addDependency(frontend.getValue(), payment.getValue());
		}
		if (!serviceDependenciesService.hasDependency(frontend.getKey(), carts.getKey())) {
			serviceDependenciesService.addDependency(frontend.getValue(), carts.getValue());
		}
		if (!serviceDependenciesService.hasDependency(frontend.getKey(), orders.getKey())) {
			serviceDependenciesService.addDependency(frontend.getValue(), orders.getValue());
		}
		if (!serviceDependenciesService.hasDependency(user.getKey(), userDb.getKey())) {
			serviceDependenciesService.addDependency(user.getValue(), userDb.getValue());
		}
		if (!serviceDependenciesService.hasDependency(catalogue.getKey(), catalogueDb.getKey())) {
			serviceDependenciesService.addDependency(catalogue.getValue(), catalogueDb.getValue());
		}
		if (!serviceDependenciesService.hasDependency(carts.getKey(), cartsDb.getKey())) {
			serviceDependenciesService.addDependency(carts.getValue(), cartsDb.getValue());
		}
		if (!serviceDependenciesService.hasDependency(orders.getKey(), payment.getKey())) {
			serviceDependenciesService.addDependency(orders.getValue(), payment.getValue());
		}
		if (!serviceDependenciesService.hasDependency(orders.getKey(), shipping.getKey())) {
			serviceDependenciesService.addDependency(orders.getValue(), shipping.getValue());
		}
		if (!serviceDependenciesService.hasDependency(orders.getKey(), ordersDb.getKey())) {
			serviceDependenciesService.addDependency(orders.getValue(), ordersDb.getValue());
		}
		if (!serviceDependenciesService.hasDependency(shipping.getKey(), rabbitmq.getKey())) {
			serviceDependenciesService.addDependency(shipping.getValue(), rabbitmq.getValue());
		}
		if (!serviceDependenciesService.hasDependency(queueMaster.getKey(), rabbitmq.getKey())) {
			serviceDependenciesService.addDependency(queueMaster.getValue(), rabbitmq.getValue());
		}

		return servicesMap;
	}

	@Transactional
	Map<String, User> loadUsers(UsersService usersService) {
		Map<String, User> usersMap = new HashMap<>(2);

		User admin;
		try {
			admin = usersService.getUser("admin");
		}
		catch (EntityNotFoundException e) {
			admin = User.builder()
				.firstName("admin")
				.lastName("admin")
				.username("admin")
				.password("admin")
				.email("admin@admin.pt")
				.role(UserRoleEnum.ROLE_SYS_ADMIN)
				.build();
			admin = usersService.addUser(admin);
		}
		usersMap.put("admin", admin);

		User danielfct;
		try {
			danielfct = usersService.getUser("danielfct");
		}
		catch (EntityNotFoundException e) {
			danielfct = User.builder()
				.firstName("daniel")
				.lastName("pimenta")
				.username("danielfct")
				.password("danielfct")
				.email("d.pimenta@campus.fct.unl.pt")
				.role(UserRoleEnum.ROLE_SYS_ADMIN)
				.build();
			danielfct = usersService.addUser(danielfct);
		}
		usersMap.put("danielfct", danielfct);

		return usersMap;
	}

	@Transactional
	Map<String, Service> loadSystemComponents(String dockerHubUsername, ServicesService servicesService,
											  ServiceDependenciesService serviceDependenciesService,
											  DockerProperties dockerProperties) {
		Map<String, Service> servicesMap = new HashMap<>(7);

		Service masterManager;
		try {
			masterManager = servicesService.getService(ServiceConstants.Name.MASTER_MANAGER);
		}
		catch (EntityNotFoundException ignored) {
			masterManager = Service.builder()
				.serviceName(ServiceConstants.Name.MASTER_MANAGER)
				.dockerRepository(dockerHubUsername + "/manager-master")
				.defaultExternalPort(8080)
				.defaultInternalPort(8080)
				.launchCommand("${registrationHost} ${externalPort} ${internalPort} ${hostname}")
				.outputLabel("${masterManagerHost}")
				.serviceType(ServiceTypeEnum.SYSTEM)
				.build();
			masterManager = servicesService.addService(masterManager);
		}
		servicesMap.put(ServiceConstants.Name.MASTER_MANAGER, masterManager);

		Service workerManager;
		try {
			workerManager = servicesService.getService(ServiceConstants.Name.WORKER_MANAGER);
		}
		catch (EntityNotFoundException ignored) {
			workerManager = Service.builder()
				.serviceName(ServiceConstants.Name.WORKER_MANAGER)
				.dockerRepository(dockerHubUsername + "/manager-worker")
				.defaultExternalPort(8081)
				.defaultInternalPort(8081)
				.outputLabel("${workerManagerHost}")
				.serviceType(ServiceTypeEnum.SYSTEM)
				.volumes(Set.of("/var/run/docker.sock:/var/run/docker.sock"))
				.expectedMemoryConsumption(620482560d)
				.build();
			workerManager = servicesService.addService(workerManager);
		}
		servicesMap.put(ServiceConstants.Name.WORKER_MANAGER, workerManager);

		/*Service dockerApiProxy;
		try {
			dockerApiProxy = servicesService.getService(ServiceConstants.Name.DOCKER_API_PROXY);
		}
		catch (EntityNotFoundException ignored) {
			dockerApiProxy = Service.builder()
				.serviceName(ServiceConstants.Name.DOCKER_API_PROXY)
				.dockerRepository(dockerHubUsername + "/nginx-basic-auth-proxy")
				.defaultExternalPort(dockerProperties.getApiProxy().getPort())
				.defaultInternalPort(80)
				.outputLabel("${dockerApiProxyHost}")
				.serviceType(ServiceTypeEnum.SYSTEM)
				.expectedMemoryConsumption(10485760d)
				.build();
			dockerApiProxy = servicesService.addService(dockerApiProxy);
		}
		servicesMap.put(ServiceConstants.Name.DOCKER_API_PROXY, dockerApiProxy);*/

		Service loadBalancer;
		try {
			loadBalancer = servicesService.getService(ServiceConstants.Name.LOAD_BALANCER);
		}
		catch (EntityNotFoundException ignored) {
			loadBalancer = Service.builder()
				.serviceName(ServiceConstants.Name.LOAD_BALANCER)
				.dockerRepository(dockerHubUsername + "/nginx-load-balancer")
				.defaultExternalPort(1906)
				.defaultInternalPort(80)
				.outputLabel("${loadBalancerHost}")
				.serviceType(ServiceTypeEnum.SYSTEM)
				.expectedMemoryConsumption(10485760d)
				.build();
			loadBalancer = servicesService.addService(loadBalancer);
		}
		servicesMap.put(ServiceConstants.Name.LOAD_BALANCER, loadBalancer);

		/*Service requestLocationMonitor;
		try {
			requestLocationMonitor = servicesService.getService(LocationRequestsService.REQUEST_LOCATION_MONITOR);
		}
		catch (EntityNotFoundException ignored) {
			requestLocationMonitor = Service.builder()
				.serviceName(LocationRequestsService.REQUEST_LOCATION_MONITOR)
				.dockerRepository(dockerHubUsername + "/request-location-monitor")
				.defaultExternalPort(1919)
				.defaultInternalPort(1919)
				.outputLabel("${requestLocationMonitorHost}")
				.serviceType(ServiceTypeEnum.SYSTEM)
				.expectedMemoryConsumption(52428800d)
				.build();
			requestLocationMonitor = servicesService.addService(requestLocationMonitor);
		}
		servicesMap.put(LocationRequestsService.REQUEST_LOCATION_MONITOR, requestLocationMonitor);*/

		Service registrationServer;
		try {
			registrationServer = servicesService.getService(ServiceConstants.Name.REGISTRATION_SERVER);
		}
		catch (EntityNotFoundException ignored) {
			registrationServer = Service.builder()
				.serviceName(ServiceConstants.Name.REGISTRATION_SERVER)
				.dockerRepository(dockerHubUsername + "/registration-server")
				.defaultExternalPort(8761)
				.defaultInternalPort(8761)
				.launchCommand("${externalPort} ${internalPort} ${hostname} ${zone}")
				.outputLabel("${registrationHost}")
				.serviceType(ServiceTypeEnum.SYSTEM)
				.expectedMemoryConsumption(262144000d)
				.build();
			registrationServer = servicesService.addService(registrationServer);
		}
		servicesMap.put(ServiceConstants.Name.REGISTRATION_SERVER, registrationServer);

		/*Service prometheus;
		try {
			prometheus = servicesService.getService(ServiceConstants.Name.PROMETHEUS);
		}
		catch (EntityNotFoundException ignored) {
			prometheus = Service.builder()
				.serviceName(ServiceConstants.Name.PROMETHEUS)
				.dockerRepository(dockerHubUsername + "/prometheus")
				.defaultExternalPort(9090)
				.defaultInternalPort(9090)
				.outputLabel("${prometheusHost}")
				.serviceType(ServiceTypeEnum.SYSTEM)
				.expectedMemoryConsumption(52428800d)
				.build();
			prometheus = servicesService.addService(prometheus);
		}
		servicesMap.put(ServiceConstants.Name.PROMETHEUS, prometheus);*/

		Service kafka;
		try {
			kafka = servicesService.getService(ServiceConstants.Name.KAFKA);
		}
		catch (EntityNotFoundException ignored) {
			kafka = Service.builder()
				.serviceName(ServiceConstants.Name.KAFKA)
				.dockerRepository("wurstmeister/kafka:2.12-2.5.0")
				.defaultExternalPort(9092)
				.defaultInternalPort(9092)
				.outputLabel("${kafkaHost}")
				.serviceType(ServiceTypeEnum.SYSTEM)
				.volumes(Set.of("/var/run/docker.sock:/var/run/docker.sock"))
				.build();
			kafka = servicesService.addService(kafka);
		}
		servicesMap.put(ServiceConstants.Name.KAFKA, kafka);

		Service zookeeper;
		try {
			zookeeper = servicesService.getService(ServiceConstants.Name.ZOOKEEPER);
		}
		catch (EntityNotFoundException ignored) {
			zookeeper = Service.builder()
				.serviceName(ServiceConstants.Name.ZOOKEEPER)
				.dockerRepository("wurstmeister/zookeeper:3.4.6")
				.defaultExternalPort(2181)
				.defaultInternalPort(2181)
				.outputLabel("${zookeeperHost}")
				.serviceType(ServiceTypeEnum.SYSTEM)
				.build();
			zookeeper = servicesService.addService(zookeeper);
		}
		servicesMap.put(ServiceConstants.Name.ZOOKEEPER, zookeeper);

		if (!serviceDependenciesService.hasDependency(ServiceConstants.Name.KAFKA, ServiceConstants.Name.ZOOKEEPER)) {
			serviceDependenciesService.addDependency(kafka, zookeeper);
		}

		return servicesMap;
	}

}