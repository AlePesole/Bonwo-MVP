package com.alessandropesole.bonwoapp.routine.domain.model;

import com.alessandropesole.bonwoapp.routine.domain.exception.InvalidSetConfigException;
import lombok.Getter;

import java.time.Duration;

@Getter
public final class SetConfig {

    private final SetType type;
    private final int reps;
    private final Double weightKg;
    private final WeightMode weightMode;
    private final Duration duration;

    private SetConfig(SetType type, int reps, Double weightKg,
                      WeightMode weightMode, Duration duration) {
        if (weightKg != null && weightKg < 0) {
            throw new InvalidSetConfigException("weightKg cannot be negative");
        }
        this.type = type;
        this.reps = reps;
        this.weightKg = weightKg;
        this.weightMode = weightKg != null ? (weightMode != null ? weightMode : WeightMode.TOTAL) : null;
        this.duration = duration;
    }

    public static SetConfig reps(int reps, Double weightKg, WeightMode weightMode) {
        if (reps <= 0) throw new InvalidSetConfigException("Reps must be > 0");
        return new SetConfig(SetType.REPS, reps, weightKg, weightMode, null);
    }

    public static SetConfig timed(Duration duration, Double weightKg, WeightMode weightMode) {
        requirePositiveDuration(duration, "TIMED");
        return new SetConfig(SetType.TIMED, 0, weightKg, weightMode, duration);
    }

    public static SetConfig amrap(Duration timeWindow) {
        requirePositiveDuration(timeWindow, "AMRAP");
        return new SetConfig(SetType.AMRAP, 0, null, null, timeWindow);
    }

    public static SetConfig toFailure(Double weightKg, WeightMode weightMode) {
        return new SetConfig(SetType.FAILURE, 0, weightKg, weightMode, null);
    }

    public Double totalWeightKg() {
        if (weightKg == null) return null;
        return weightMode == WeightMode.PER_SIDE ? weightKg * 2 : weightKg;
    }

    private static void requirePositiveDuration(Duration d, String type) {
        if (d == null || d.isNegative() || d.isZero()) {
            throw new InvalidSetConfigException("Duration must be positive for " + type + " sets");
        }
    }
}
