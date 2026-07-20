package com.alessandropesole.bonwoapp.exercise.domain.model;

import com.alessandropesole.bonwoapp.exercise.domain.exception.InvalidMuscleEntryException;
import lombok.Getter;

@Getter
public final class MuscleEntry {

    private static final double MIN = 0.1;
    private static final double MAX = 1.0;

    private final Long subGroupId;
    private final double activation;

    private MuscleEntry(Long subGroupId, double activation) {
        this.subGroupId = subGroupId;
        this.activation = activation;
    }

    public static MuscleEntry of(Long subGroupId, double activation) {
        if (subGroupId == null)
            throw new InvalidMuscleEntryException("subGroupId is required");
        if (activation < MIN || activation > MAX)
            throw new InvalidMuscleEntryException(
                    "activation must be between " + MIN + " and " + MAX +
                            " (0.0 means the muscle is not used — omit it instead)");
        return new MuscleEntry(subGroupId, Math.round(activation * 10.0) / 10.0);
    }

    public ActivationLevel role() {
        return ActivationLevel.from(activation);
    }
}
