package com.alessandropesole.bonwoapp.routine.application.mapper;

import com.alessandropesole.bonwoapp.exercise.application.dto.ExerciseResponse;
import com.alessandropesole.bonwoapp.routine.application.dto.ExerciseSlotDto;
import com.alessandropesole.bonwoapp.routine.application.dto.ExerciseSlotResponse;
import com.alessandropesole.bonwoapp.routine.domain.model.ExerciseSlot;

import java.util.List;

public final class ExerciseSlotDtoMapper {

    private ExerciseSlotDtoMapper() {
    }

    public static ExerciseSlot toDomain(ExerciseSlotDto dto) {
        return ExerciseSlot.create(
                dto.exerciseId(), dto.position(),
                SetConfigDtoMapper.toDomainList(dto.sets()),
                dto.restBetweenSets());
    }

    public static List<ExerciseSlot> toDomainList(List<ExerciseSlotDto> dtos) {
        if (dtos == null) return List.of();
        return dtos.stream().map(ExerciseSlotDtoMapper::toDomain).toList();
    }

    public static ExerciseSlotResponse toResponse(ExerciseSlot slot, ExerciseResponse exercise) {
        var sets = slot.getSets().stream().map(SetConfigDtoMapper::toResponse).toList();
        return new ExerciseSlotResponse(
                slot.getExerciseId(), exercise, slot.getPosition(), sets, slot.getRestBetweenSets());
    }
}
