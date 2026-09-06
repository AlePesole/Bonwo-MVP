package com.alessandropesole.bonwoapp.routine.domain.model;

import java.util.Set;

public record RoutineFilter(
        Long muscleGroupId,
        Long muscleSubGroupId,
        Set<Long> equipmentIds,
        Set<Long> activityIds,
        Set<Long> trainingGoalIds,
        String title
) {}
