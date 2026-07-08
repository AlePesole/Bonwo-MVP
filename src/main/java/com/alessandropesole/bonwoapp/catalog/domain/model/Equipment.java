package com.alessandropesole.bonwoapp.catalog.domain.model;

import com.alessandropesole.bonwoapp.catalog.domain.exception.InvalidEquipmentNameException;
import lombok.Getter;

@Getter
public class Equipment {

    private Long id;
    private String name;
    private Long iconId;

    public static Equipment create(String name, Long iconId) {
        validateName(name);
        Equipment e = new Equipment();
        e.name = name.trim();
        e.iconId = iconId;
        return e;
    }

    public static Equipment reconstitute(Long id, String name, Long iconId) {
        Equipment e = new Equipment();
        e.id = id;
        e.name = name;
        e.iconId = iconId;
        return e;
    }

    public void update(String name, Long iconId) {
        if (name != null) {
            validateName(name);
            this.name = name.trim();
        }
        if (iconId != null) this.iconId = iconId;
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank() || name.length() > 100) {
            throw new InvalidEquipmentNameException("Equipment name must be 1-100 characters");
        }
    }

}
