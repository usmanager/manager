package pt.unl.fct.miei.usmanagement.manager.dtos.mapper;

import org.mapstruct.Builder;
import org.mapstruct.Context;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueMappingStrategy;
import org.mapstruct.factory.Mappers;
import pt.unl.fct.miei.usmanagement.manager.dtos.kafka.ConditionDTO;
import pt.unl.fct.miei.usmanagement.manager.rulesystem.condition.Condition;

@Mapper(builder = @Builder(disableBuilder = true))
public interface ConditionMapper {

	ConditionMapper MAPPER = Mappers.getMapper(ConditionMapper.class);

	@IterableMapping(nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
	Condition toCondition(ConditionDTO conditionDTO, @Context CycleAvoidingMappingContext context);

	@InheritInverseConfiguration
	ConditionDTO fromCondition(Condition condition, @Context CycleAvoidingMappingContext context);

}
