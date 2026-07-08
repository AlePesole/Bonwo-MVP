package com.alessandropesole.bonwoapp.catalog.domain.model;

import com.alessandropesole.bonwoapp.catalog.domain.exception.InvalidTrainingGoalNameException;
import lombok.Getter;

@Getter
public class TrainingGoal {

    private Long id;
    private String name;
    private String detail;
    private Long iconId;

    public static TrainingGoal create(String name, String detail, Long iconId) {
        validateName(name);
        if (iconId == null) throw new IllegalArgumentException("iconId is required");
        TrainingGoal t = new TrainingGoal();
        t.name    = name.trim();
        t.detail  = detail;
        t.iconId = iconId;
        return t;
    }

    public static TrainingGoal reconstitute(Long id, String name, String detail, Long iconId) {
        TrainingGoal t = new TrainingGoal();
        t.id      = id;
        t.name    = name;
        t.detail  = detail;
        t.iconId = iconId;
        return t;
    }

    public void update(String name, String detail, Long iconId) {
        if (name   != null) { validateName(name); this.name = name.trim(); }
        if (detail != null) this.detail  = detail;
        if (iconId != null) this.iconId = iconId;
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank() || name.length() > 100) {
            throw new InvalidTrainingGoalNameException("Training goal name must be 1-100 characters");
        }
    }
}
