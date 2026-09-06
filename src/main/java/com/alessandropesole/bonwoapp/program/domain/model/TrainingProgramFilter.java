package com.alessandropesole.bonwoapp.program.domain.model;

import java.util.Set;

public record TrainingProgramFilter(
        Set<Long> equipmentIds,
        Set<Long> activityIds,
        Set<Long> trainingGoalIds,
        String title
) {}
