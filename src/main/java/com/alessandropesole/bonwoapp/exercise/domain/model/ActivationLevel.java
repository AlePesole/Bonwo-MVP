package com.alessandropesole.bonwoapp.exercise.domain.model;

public enum ActivationLevel {
    STABILIZER,
    SECONDARY,
    PRIMARY;

    public static final double PRIMARY_THRESHOLD = 0.7;
    public static final double SECONDARY_THRESHOLD = 0.3;

    public static ActivationLevel from(double activation) {
        if (activation >= PRIMARY_THRESHOLD) return PRIMARY;
        if (activation >= SECONDARY_THRESHOLD) return SECONDARY;
        return STABILIZER;
    }
}
