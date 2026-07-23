package com.alessandropesole.bonwoapp.routine.domain.model;

import com.alessandropesole.bonwoapp.routine.domain.exception.InvalidSlotException;
import lombok.Getter;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public final class ExerciseSlot {

    private final Long exerciseId;
    private final int position;
    private final List<SetConfig> sets;
    private final Duration restBetweenSets;

    private ExerciseSlot(Long exerciseId, int position,
                         List<SetConfig> sets, Duration restBetweenSets) {
        this.exerciseId = exerciseId;
        this.position = position;
        this.sets = Collections.unmodifiableList(new ArrayList<>(sets));
        this.restBetweenSets = restBetweenSets;
    }

    public static ExerciseSlot create(Long exerciseId, int position,
                                      List<SetConfig> sets, Duration restBetweenSets) {
        validateSlot(exerciseId, sets, position);
        return new ExerciseSlot(exerciseId, position, sets, restBetweenSets);
    }

    public static ExerciseSlot reconstitute(Long exerciseId, int position,
                                            List<SetConfig> sets, Duration restBetweenSets) {
        return new ExerciseSlot(exerciseId, position, sets, restBetweenSets);
    }

    public ExerciseSlot withSets(List<SetConfig> newSets, Duration newRest) {
        if (newSets == null || newSets.isEmpty()) throw new InvalidSlotException("A slot must have at least one set");
        return new ExerciseSlot(exerciseId, position, newSets, newRest);
    }

    private static void validateSlot(Long exerciseId, List<SetConfig> sets, int position) {
        if (exerciseId == null) throw new InvalidSlotException("exerciseId is required");
        if (sets == null || sets.isEmpty()) throw new InvalidSlotException("A slot must have at least one set");
        if (position < 1) throw new InvalidSlotException("Position must be >= 1");
    }
}
