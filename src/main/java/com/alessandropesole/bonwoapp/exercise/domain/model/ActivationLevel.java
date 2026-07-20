package com.alessandropesole.bonwoapp.exercise.domain.model;

public enum ActivationLevel {
    STABILIZER,
    SECONDARY,
    PRIMARY;

    public static ActivationLevel from(double activation) {
        if (activation >= 0.7) return PRIMARY;
        if (activation >= 0.3) return SECONDARY;
        return STABILIZER;
    }
}
