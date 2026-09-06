package com.alessandropesole.bonwoapp.session.application.mapper;

import com.alessandropesole.bonwoapp.exercise.application.dto.ExerciseResponse;
import com.alessandropesole.bonwoapp.session.application.dto.TrainingSlotDto;
import com.alessandropesole.bonwoapp.session.application.dto.TrainingSlotResponse;
import com.alessandropesole.bonwoapp.session.domain.model.TrainingSlot;

import java.util.List;

public final class TrainingSlotDtoMapper {

    private TrainingSlotDtoMapper() {
    }

    public static TrainingSlot toDomain(TrainingSlotDto dto) {
        return TrainingSlot.create(
                dto.exerciseId(), dto.position(),
                TrainingSetDtoMapper.toDomainList(dto.sets()),
                dto.restBetweenSets());
    }

    public static List<TrainingSlot> toDomainList(List<TrainingSlotDto> dtos) {
        if (dtos == null) return List.of();
        return dtos.stream().map(TrainingSlotDtoMapper::toDomain).toList();
    }

    public static TrainingSlotResponse toResponse(TrainingSlot slot, ExerciseResponse exercise) {
        var sets = slot.getSets().stream().map(TrainingSetDtoMapper::toResponse).toList();
        return new TrainingSlotResponse(
                slot.getExerciseId(), exercise, slot.getPosition(), sets,
                slot.getRestBetweenSets(), slot.isDone());
    }
}
