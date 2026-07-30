package com.alessandropesole.bonwoapp.routine.infrastructure.persistence.mapper;

import com.alessandropesole.bonwoapp.exercise.application.service.MuscleSummaryJsonMapper;
import com.alessandropesole.bonwoapp.routine.domain.model.Routine;
import com.alessandropesole.bonwoapp.routine.infrastructure.persistence.entity.RoutineJpaEntity;

import java.util.ArrayList;
import java.util.LinkedHashSet;

public final class RoutineMapper {

    private RoutineMapper() {
    }

    public static Routine toDomain(RoutineJpaEntity e) {
        var slots = e.getSlots().stream().map(ExerciseSlotMapper::toDomain).toList();
        return Routine.reconstitute(
                e.getId(), e.getOwnerId(), e.getTitle(), e.getDescription(),
                e.getLevel(), e.getThumbnailId(), e.getEstimatedDuration(),
                slots,
                e.getRestBetweenExercises(),
                MuscleSummaryJsonMapper.fromJson(e.getMuscleSummaryJson()),
                new LinkedHashSet<>(e.getEquipmentIds()),
                new LinkedHashSet<>(e.getActivityIds()),
                new LinkedHashSet<>(e.getTrainingGoalIds()),
                e.getCreatedAt(), e.getTrainingProgramId(), e.getPosition()
        );
    }

    public static RoutineJpaEntity toEntity(Routine r) {
        RoutineJpaEntity entity = RoutineJpaEntity.builder()
                .id(r.getId())
                .ownerId(r.getOwnerId())
                .title(r.getTitle())
                .description(r.getDescription())
                .level(r.getLevel())
                .thumbnailId(r.getThumbnailId())
                .estimatedDuration(r.getEstimatedDuration())
                .restBetweenExercises(r.getRestBetweenExercises())
                .muscleSummaryJson(MuscleSummaryJsonMapper.toJson(r.getMuscleSummary()))
                .equipmentIds(new ArrayList<>(r.getEquipmentIds()))
                .activityIds(new ArrayList<>(r.getActivityIds()))
                .trainingGoalIds(new ArrayList<>(r.getTrainingGoalIds()))
                .createdAt(r.getCreatedAt())
                .trainingProgramId(r.getTrainingProgramId())
                .position(r.getPosition())
                .build();

        var slots = r.getSlots().stream()
                .map(slot -> ExerciseSlotMapper.toEntity(slot, entity))
                .toList();
        entity.getSlots().addAll(slots);
        return entity;
    }
}
