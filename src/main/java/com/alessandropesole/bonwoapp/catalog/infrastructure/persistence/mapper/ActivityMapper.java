package com.alessandropesole.bonwoapp.catalog.infrastructure.persistence.mapper;

import com.alessandropesole.bonwoapp.catalog.domain.model.Activity;
import com.alessandropesole.bonwoapp.catalog.infrastructure.persistence.entity.ActivityJpaEntity;

public final class ActivityMapper {

    private ActivityMapper() {}

    public static Activity toDomain(ActivityJpaEntity a) {
        return Activity.reconstitute(a.getId(), a.getName(), a.getDetail(), a.getIconId());
    }

    public static ActivityJpaEntity toEntity(Activity a) {
        return ActivityJpaEntity.builder()
                .id(a.getId())
                .name(a.getName())
                .detail(a.getDetail())
                .iconId(a.getIconId())
                .build();
    }
}
