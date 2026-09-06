package com.alessandropesole.bonwoapp.session.infrastructure.persistence.json;

import java.time.Duration;
import java.util.List;

/** Plain, trivially-Jackson-serializable mirror of TrainingSlot — see TrainingSetJson. */
public record TrainingSlotJson(
        Long exerciseId,
        int position,
        List<TrainingSetJson> sets,
        Duration restBetweenSets
) {}
