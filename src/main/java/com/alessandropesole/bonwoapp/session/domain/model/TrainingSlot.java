package com.alessandropesole.bonwoapp.session.domain.model;

import com.alessandropesole.bonwoapp.session.domain.exception.InvalidTrainingSlotException;
import lombok.Getter;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An exercise slot within a TrainingSession. Unlike the routine module's ExerciseSlot (a fixed plan),
 * this can be freely added/removed during the session, and its `done` status is never stored directly —
 * it's always derived from whether every one of its sets is done.
 */
@Getter
public final class TrainingSlot {

    private final Long exerciseId;
    private final int position;
    private final List<TrainingSet> sets;
    private final Duration restBetweenSets;

    private TrainingSlot(Long exerciseId, int position,
                         List<TrainingSet> sets, Duration restBetweenSets) {
        this.exerciseId = exerciseId;
        this.position = position;
        this.sets = Collections.unmodifiableList(new ArrayList<>(sets));
        this.restBetweenSets = restBetweenSets;
    }

    public static TrainingSlot create(Long exerciseId, int position,
                                      List<TrainingSet> sets, Duration restBetweenSets) {
        validateSlot(exerciseId, sets, position);
        return new TrainingSlot(exerciseId, position, sets, restBetweenSets);
    }

    public static TrainingSlot reconstitute(Long exerciseId, int position,
                                            List<TrainingSet> sets, Duration restBetweenSets) {
        return new TrainingSlot(exerciseId, position, sets, restBetweenSets);
    }

    /** An exercise is done once every one of its sets is marked done. Never manually settable. */
    public boolean isDone() {
        return !sets.isEmpty() && sets.stream().allMatch(TrainingSet::isDone);
    }

    private static void validateSlot(Long exerciseId, List<TrainingSet> sets, int position) {
        if (exerciseId == null) throw new InvalidTrainingSlotException("exerciseId is required");
        if (sets == null || sets.isEmpty()) throw new InvalidTrainingSlotException("A slot must have at least one set");
        if (position < 1) throw new InvalidTrainingSlotException("Position must be >= 1");
    }
}
