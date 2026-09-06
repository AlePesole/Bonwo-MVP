package com.alessandropesole.bonwoapp.session.application.dto;

import com.alessandropesole.bonwoapp.exercise.application.dto.ExerciseResponse;

import java.time.Duration;
import java.util.List;

public record TrainingSlotResponse(
        Long exerciseId,
        ExerciseResponse exercise,     // resolved lazily — null if the exercise was deleted
        int position,
        List<TrainingSetResponse> sets,
        Duration restBetweenSets,
        boolean done                   // derived — true once every set in this slot is done
) {}
