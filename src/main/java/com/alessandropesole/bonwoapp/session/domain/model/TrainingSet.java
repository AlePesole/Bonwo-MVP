package com.alessandropesole.bonwoapp.session.domain.model;

import com.alessandropesole.bonwoapp.routine.domain.model.SetType;
import com.alessandropesole.bonwoapp.routine.domain.model.WeightMode;
import com.alessandropesole.bonwoapp.session.domain.exception.InvalidTrainingSetException;
import lombok.Getter;

import java.time.Duration;

/**
 * A single set actually performed during a TrainingSession. Same shape as the routine module's
 * SetConfig (the plan), plus a `done` flag that tracks whether this particular set was completed.
 */
@Getter
public final class TrainingSet {

    private final SetType type;
    private final int reps;
    private final Double weightKg;
    private final WeightMode weightMode;
    private final Duration duration;
    private final boolean done;

    private TrainingSet(SetType type, int reps, Double weightKg,
                        WeightMode weightMode, Duration duration, boolean done) {
        if (weightKg != null && weightKg < 0) {
            throw new InvalidTrainingSetException("weightKg cannot be negative");
        }
        this.type = type;
        this.reps = reps;
        this.weightKg = weightKg;
        this.weightMode = weightKg != null ? (weightMode != null ? weightMode : WeightMode.TOTAL) : null;
        this.duration = duration;
        this.done = done;
    }

    public static TrainingSet reps(int reps, Double weightKg, WeightMode weightMode, boolean done) {
        if (reps <= 0) throw new InvalidTrainingSetException("Reps must be > 0");
        return new TrainingSet(SetType.REPS, reps, weightKg, weightMode, null, done);
    }

    public static TrainingSet timed(Duration duration, Double weightKg, WeightMode weightMode, boolean done) {
        requirePositiveDuration(duration, "TIMED");
        return new TrainingSet(SetType.TIMED, 0, weightKg, weightMode, duration, done);
    }

    public static TrainingSet amrap(Duration timeWindow, boolean done) {
        requirePositiveDuration(timeWindow, "AMRAP");
        return new TrainingSet(SetType.AMRAP, 0, null, null, timeWindow, done);
    }

    public static TrainingSet toFailure(Double weightKg, WeightMode weightMode, boolean done) {
        return new TrainingSet(SetType.FAILURE, 0, weightKg, weightMode, null, done);
    }

    public Double totalWeightKg() {
        if (weightKg == null) return null;
        return weightMode == WeightMode.PER_SIDE ? weightKg * 2 : weightKg;
    }

    private static void requirePositiveDuration(Duration d, String type) {
        if (d == null || d.isNegative() || d.isZero()) {
            throw new InvalidTrainingSetException("Duration must be positive for " + type + " sets");
        }
    }
}
