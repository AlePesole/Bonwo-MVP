package com.alessandropesole.bonwoapp.program.infrastructure.persistence.mapper;

import com.alessandropesole.bonwoapp.exercise.application.service.MuscleSummaryJsonMapper;
import com.alessandropesole.bonwoapp.program.domain.model.TrainingProgram;
import com.alessandropesole.bonwoapp.program.infrastructure.persistence.entity.TrainingProgramJpaEntity;

import java.util.ArrayList;
import java.util.LinkedHashSet;

public final class TrainingProgramMapper {

    private TrainingProgramMapper() {
    }

    public static TrainingProgram toDomain(TrainingProgramJpaEntity e) {
        return TrainingProgram.reconstitute(
                e.getId(), e.getOwnerId(), e.getTitle(), e.getDescription(),
                e.getLevel(), e.getThumbnailId(), e.getDaysPerWeek(),
                MuscleSummaryJsonMapper.fromJson(e.getMuscleSummaryJson()),
                new LinkedHashSet<>(e.getEquipmentIds()),
                new LinkedHashSet<>(e.getActivityIds()),
                new LinkedHashSet<>(e.getTrainingGoalIds()),
                e.getCreatedAt()
        );
    }

    public static TrainingProgramJpaEntity toEntity(TrainingProgram p) {
        return TrainingProgramJpaEntity.builder()
                .id(p.getId())
                .ownerId(p.getOwnerId())
                .title(p.getTitle())
                .description(p.getDescription())
                .level(p.getLevel())
                .thumbnailId(p.getThumbnailId())
                .daysPerWeek(p.getDaysPerWeek())
                .muscleSummaryJson(MuscleSummaryJsonMapper.toJson(p.getMuscleSummary()))
                .equipmentIds(new ArrayList<>(p.getEquipmentIds()))
                .activityIds(new ArrayList<>(p.getActivityIds()))
                .trainingGoalIds(new ArrayList<>(p.getTrainingGoalIds()))
                .createdAt(p.getCreatedAt())
                .build();
    }
}
