package com.alessandropesole.bonwoapp.program.application.dto;

import com.alessandropesole.bonwoapp.catalog.application.dto.ActivityResponse;
import com.alessandropesole.bonwoapp.catalog.application.dto.EquipmentResponse;
import com.alessandropesole.bonwoapp.catalog.application.dto.TrainingGoalResponse;
import com.alessandropesole.bonwoapp.exercise.domain.model.Level;
import com.alessandropesole.bonwoapp.media.application.dto.ImageResponse;
import com.alessandropesole.bonwoapp.routine.application.dto.RoutineResponse;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record TrainingProgramResponse(
        Long id,
        Long ownerId,
        String title,
        String description,
        Level level,
        ImageResponse thumbnail,
        int daysPerWeek,
        List<RoutineResponse> routines,
        Map<Long, Double> muscleSummary,
        List<EquipmentResponse> equipment,
        List<ActivityResponse> activities,
        List<TrainingGoalResponse> trainingGoals,
        Instant createdAt
) {}
