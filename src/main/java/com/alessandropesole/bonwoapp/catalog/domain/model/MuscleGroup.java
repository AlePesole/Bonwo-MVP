package com.alessandropesole.bonwoapp.catalog.domain.model;

import com.alessandropesole.bonwoapp.catalog.domain.exception.InvalidCatalogNameException;
import lombok.Getter;

@Getter
public class MuscleGroup {

    private Long id;
    private String name;
    private Long iconId;

    public static MuscleGroup create(String name, Long iconId) {
        validateName(name);
        if (iconId == null) throw new IllegalArgumentException("iconId is required");
        MuscleGroup g = new MuscleGroup();
        g.name = name.strip();
        g.iconId = iconId;
        return g;
    }

    public static MuscleGroup reconstitute(Long id, String name, Long iconId) {
        MuscleGroup g = new MuscleGroup();
        g.id = id;
        g.name = name;
        g.iconId = iconId;
        return g;
    }

    public void update(String name, Long iconId) {
        if (name != null) {
            validateName(name);
            this.name = name.strip();
        }
        if (iconId != null) this.iconId = iconId;
    }

    private static void validateName(String name) {
        if (name == null || name.strip().length() < 3 || name.strip().length() > 100)
            throw new InvalidCatalogNameException("Muscle group name must be 3-100 characters");
    }
}
