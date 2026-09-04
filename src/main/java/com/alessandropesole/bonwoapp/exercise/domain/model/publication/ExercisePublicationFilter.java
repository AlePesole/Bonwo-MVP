package com.alessandropesole.bonwoapp.exercise.domain.model.publication;

import java.util.Set;

public record ExercisePublicationFilter(
        Long muscleGroupId,
        Long muscleSubGroupId,
        Set<Long> equipmentIds,
        Set<Long> activityIds,
        Set<Long> trainingGoalIds,
        PublicationType type,
        PublicationSort sort,
        String title
) {}
