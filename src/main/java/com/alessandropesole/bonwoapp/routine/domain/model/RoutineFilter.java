package com.alessandropesole.bonwoapp.routine.domain.model;

import java.util.Set;

public record RoutineFilter(
        Set<Long> equipmentIds,
        Set<Long> activityIds,
        Set<Long> trainingGoalIds
) {}