package com.alessandropesole.bonwoapp.session.infrastructure.persistence.json;

import java.time.Duration;
import java.util.List;

public record TrainingSlotJson(
        Long exerciseId,
        int position,
        List<TrainingSetJson> sets,
        Duration restBetweenSets
) {}
