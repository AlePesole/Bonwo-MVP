package com.alessandropesole.bonwoapp.session.infrastructure.persistence.mapper;

import com.alessandropesole.bonwoapp.exercise.application.service.MuscleSummaryJsonMapper;
import com.alessandropesole.bonwoapp.session.domain.model.TrainingSession;
import com.alessandropesole.bonwoapp.session.infrastructure.persistence.entity.TrainingSessionJpaEntity;

public final class TrainingSessionMapper {

    private TrainingSessionMapper() {
    }

    public static TrainingSession toDomain(TrainingSessionJpaEntity e) {
        return TrainingSession.reconstitute(
                e.getId(), e.getOwnerId(), e.getRoutineId(), e.getRoutineTitle(), e.getTrainingProgramId(),
                e.getStatus(), e.getStartedAt(), e.getCompletedAt(), e.getDuration(), e.getFinalNote(),
                TrainingSlotJsonMapper.fromJson(e.getSlotsJson()),
                MuscleSummaryJsonMapper.fromJson(e.getMuscleSummaryJson())
        );
    }

    public static TrainingSessionJpaEntity toEntity(TrainingSession s) {
        return TrainingSessionJpaEntity.builder()
                .id(s.getId())
                .ownerId(s.getOwnerId())
                .routineId(s.getRoutineId())
                .routineTitle(s.getRoutineTitle())
                .trainingProgramId(s.getTrainingProgramId())
                .status(s.getStatus())
                .startedAt(s.getStartedAt())
                .completedAt(s.getCompletedAt())
                .duration(s.getDuration())
                .finalNote(s.getFinalNote())
                .slotsJson(TrainingSlotJsonMapper.toJson(s.getSlots()))
                .muscleSummaryJson(MuscleSummaryJsonMapper.toJson(s.getMuscleSummary()))
                .build();
    }
}
