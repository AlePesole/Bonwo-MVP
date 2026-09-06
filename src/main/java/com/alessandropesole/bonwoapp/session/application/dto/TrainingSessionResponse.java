package com.alessandropesole.bonwoapp.session.application.dto;

import com.alessandropesole.bonwoapp.session.domain.model.SessionStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record TrainingSessionResponse(
        Long id,
        Long ownerId,
        Long routineId,
        String routineTitle,
        Long trainingProgramId,
        SessionStatus status,
        Instant startedAt,
        Instant completedAt,
        Duration duration,
        String finalNote,
        List<TrainingSlotResponse> slots,
        Map<Long, Double> muscleSummary
) {}
