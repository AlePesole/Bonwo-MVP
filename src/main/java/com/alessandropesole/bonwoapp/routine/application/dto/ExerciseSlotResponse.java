package com.alessandropesole.bonwoapp.routine.application.dto;

import com.alessandropesole.bonwoapp.exercise.application.dto.ExerciseResponse;

import java.time.Duration;
import java.util.List;

public record ExerciseSlotResponse(
        Long exerciseId,
        ExerciseResponse exercise,
        int position,
        List<SetConfigResponse> sets,
        Duration restBetweenSets
) {}
