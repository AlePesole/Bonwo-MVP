package com.alessandropesole.bonwoapp.exercise.domain.model;

import java.util.Set;

public record ExerciseFilter(
        Long muscleGroupId,
        Long muscleSubGroupId,
        Set<Long> equipmentIds,
        Set<Long> activityIds,
        Set<Long> trainingGoalIds,
        String title
) {}