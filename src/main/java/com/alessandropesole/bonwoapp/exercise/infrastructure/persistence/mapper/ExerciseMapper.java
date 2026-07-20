package com.alessandropesole.bonwoapp.exercise.infrastructure.persistence.mapper;

import com.alessandropesole.bonwoapp.exercise.application.service.MuscleSummaryJsonMapper;
import com.alessandropesole.bonwoapp.exercise.domain.model.Exercise;
import com.alessandropesole.bonwoapp.exercise.infrastructure.persistence.entity.ExerciseJpaEntity;

import java.util.ArrayList;
import java.util.LinkedHashSet;

public final class ExerciseMapper {

    private ExerciseMapper() {
    }

    public static Exercise toDomain(ExerciseJpaEntity e) {
        return Exercise.reconstitute(
                e.getId(), e.getOwnerId(), e.getTitle(), e.getLevel(),
                e.getThumbnailId(), e.getMainVideoId(),
                e.getDescription(), e.getInstructions(),
                MuscleEntryMapper.toDomainList(e.getMuscles()),
                MuscleSummaryJsonMapper.fromJson(e.getMuscleSummaryJson()),
                new LinkedHashSet<>(e.getEquipmentIds()),
                new LinkedHashSet<>(e.getActivityIds()),
                new LinkedHashSet<>(e.getTrainingGoalIds()),
                e.getCreatedAt()
        );
    }

    public static ExerciseJpaEntity toEntity(Exercise e) {
        return ExerciseJpaEntity.builder()
                .id(e.getId())
                .ownerId(e.getOwnerId())
                .title(e.getTitle())
                .level(e.getLevel())
                .thumbnailId(e.getThumbnailId())
                .mainVideoId(e.getMainVideoId())
                .description(e.getDescription())
                .instructions(e.getInstructions())
                .muscles(MuscleEntryMapper.toEmbeddableList(e.getMuscles()))
                .muscleSummaryJson(MuscleSummaryJsonMapper.toJson(e.getMuscleSummary()))
                .equipmentIds(new ArrayList<>(e.getEquipmentIds()))
                .activityIds(new ArrayList<>(e.getActivityIds()))
                .trainingGoalIds(new ArrayList<>(e.getTrainingGoalIds()))
                .createdAt(e.getCreatedAt())
                .build();
    }
}
