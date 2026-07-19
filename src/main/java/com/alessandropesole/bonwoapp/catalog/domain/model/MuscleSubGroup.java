package com.alessandropesole.bonwoapp.catalog.domain.model;

import com.alessandropesole.bonwoapp.catalog.domain.exception.InvalidCatalogNameException;
import lombok.Getter;

@Getter
public class MuscleSubGroup {

    private Long id;
    private Long groupId;
    private String name;
    private String detail;
    private String svgPathFront;
    private String svgPathBack;
    private Long iconId;

    public static MuscleSubGroup create(Long groupId, String name, String detail,
                                        String svgPathFront, String svgPathBack, Long iconId) {
        validate(groupId, name, svgPathFront, svgPathBack);
        MuscleSubGroup s = new MuscleSubGroup();
        s.groupId = groupId;
        s.name = name.strip();
        s.detail = detail;
        s.svgPathFront = svgPathFront != null ? svgPathFront.strip() : null;
        s.svgPathBack = svgPathBack != null ? svgPathBack.strip() : null;
        s.iconId = iconId;
        return s;
    }

    public static MuscleSubGroup reconstitute(Long id, Long groupId, String name, String detail,
                                              String svgPathFront, String svgPathBack, Long iconId) {
        MuscleSubGroup s = new MuscleSubGroup();
        s.id = id;
        s.groupId = groupId;
        s.name = name;
        s.detail = detail;
        s.svgPathFront = svgPathFront;
        s.svgPathBack = svgPathBack;
        s.iconId = iconId;
        return s;
    }

    public void update(String name, String detail, String svgPathFront, String svgPathBack, Long iconId) {
        if (name != null) {
            validateName(name);
            this.name = name.strip();
        }
        if (detail != null) this.detail = detail;
        if (svgPathFront != null) {
            this.svgPathFront = svgPathFront.isBlank() ? null : svgPathFront.strip();
        }
        if (svgPathBack != null) {
            this.svgPathBack = svgPathBack.isBlank() ? null : svgPathBack.strip();
        }
        if (isBlank(this.svgPathFront) && isBlank(this.svgPathBack))
            throw new InvalidCatalogNameException("At least one of svgPathFront or svgPathBack is required");
        if (iconId != null) this.iconId = iconId;
    }

    private static void validate(Long groupId, String name, String svgPathFront, String svgPathBack) {
        if (groupId == null) throw new IllegalArgumentException("groupId is required");
        validateName(name);
        if (isBlank(svgPathFront) && isBlank(svgPathBack))
            throw new InvalidCatalogNameException("At least one of svgPathFront or svgPathBack is required");
    }

    private static void validateName(String name) {
        if (name == null || name.strip().length() < 3 || name.strip().length() > 100)
            throw new InvalidCatalogNameException("Sub-group name must be 3-100 characters");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
