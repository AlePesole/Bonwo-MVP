package com.alessandropesole.bonwoapp.session.application.dto;

import com.alessandropesole.bonwoapp.exercise.application.dto.ExerciseResponse;

import java.time.Duration;
import java.util.List;

public record TrainingSlotResponse(
        Long exerciseId,
        ExerciseResponse exercise,
        int position,
        List<TrainingSetResponse> sets,
        Duration restBetweenSets,
        boolean done
) {}
