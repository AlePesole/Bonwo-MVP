package com.alessandropesole.bonwoapp.catalog.domain.model;

import com.alessandropesole.bonwoapp.catalog.domain.exception.InvalidActivityNameException;
import lombok.Getter;

@Getter
public class Activity {

    private Long id;
    private String name;
    private String detail;
    private Long iconId;

    public static Activity create(String name, String detail, Long iconId) {
        validateName(name);
        Activity a = new Activity();
        a.name = name.trim();
        a.detail = detail;
        a.iconId = iconId;
        return a;
    }

    public static Activity reconstitute(Long id, String name, String detail, Long iconId) {
        Activity a = new Activity();
        a.id = id;
        a.name = name;
        a.detail = detail;
        a.iconId = iconId;
        return a;
    }

    public void update(String name, String detail, Long iconId) {
        if (name != null) { validateName(name); this.name = name.trim(); }
        if (detail != null) this.detail = detail;
        if (iconId != null) this.iconId = iconId;
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank() || name.length() > 100) {
            throw new InvalidActivityNameException("Activity name must be 1-100 characters");
        }
    }

}
