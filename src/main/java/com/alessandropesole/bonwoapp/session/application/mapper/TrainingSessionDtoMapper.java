package com.alessandropesole.bonwoapp.session.application.mapper;

import com.alessandropesole.bonwoapp.session.application.dto.TrainingSessionResponse;
import com.alessandropesole.bonwoapp.session.application.dto.TrainingSlotResponse;
import com.alessandropesole.bonwoapp.session.domain.model.TrainingSession;

import java.util.List;

public final class TrainingSessionDtoMapper {

    private TrainingSessionDtoMapper() {
    }

    public static TrainingSessionResponse toResponse(TrainingSession s, List<TrainingSlotResponse> slots) {
        return new TrainingSessionResponse(
                s.getId(), s.getOwnerId(), s.getRoutineId(), s.getRoutineTitle(), s.getTrainingProgramId(),
                s.getStatus(), s.getStartedAt(), s.getCompletedAt(), s.getDuration(), s.getFinalNote(),
                slots, s.getMuscleSummary().getScores()
        );
    }
}
