package com.alessandropesole.bonwoapp.routine.application.dto;

import com.alessandropesole.bonwoapp.catalog.application.dto.ActivityResponse;
import com.alessandropesole.bonwoapp.catalog.application.dto.EquipmentResponse;
import com.alessandropesole.bonwoapp.catalog.application.dto.TrainingGoalResponse;
import com.alessandropesole.bonwoapp.exercise.domain.model.Level;
import com.alessandropesole.bonwoapp.media.application.dto.ImageResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record RoutineResponse(
        Long id,
        Long ownerId,
        String title,
        String description,
        Level level,
        ImageResponse thumbnail,
        Duration estimatedDuration,
        Duration restBetweenExercises,
        List<ExerciseSlotResponse> slots,
        Map<Long, Double> muscleSummary,
        List<EquipmentResponse> equipment,
        List<ActivityResponse> activities,
        List<TrainingGoalResponse> trainingGoals,
        Instant createdAt,
        Long trainingProgramId,
        Integer position
) {}
