package com.alessandropesole.bonwoapp.session.infrastructure.persistence.json;

import com.alessandropesole.bonwoapp.routine.domain.model.SetType;
import com.alessandropesole.bonwoapp.routine.domain.model.WeightMode;

import java.time.Duration;

/** Plain, trivially-Jackson-serializable mirror of TrainingSet — used only as the wire shape stored
 *  inside training_sessions.slots_json. Never exposed outside the persistence layer. */
public record TrainingSetJson(
        SetType type,
        int reps,
        Double weightKg,
        WeightMode weightMode,
        Duration duration,
        boolean done
) {}
