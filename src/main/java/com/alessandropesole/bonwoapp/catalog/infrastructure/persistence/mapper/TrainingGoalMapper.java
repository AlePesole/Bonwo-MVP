package com.alessandropesole.bonwoapp.catalog.infrastructure.persistence.mapper;

import com.alessandropesole.bonwoapp.catalog.domain.model.TrainingGoal;
import com.alessandropesole.bonwoapp.catalog.infrastructure.persistence.entity.TrainingGoalJpaEntity;

public final class TrainingGoalMapper {

    private TrainingGoalMapper() {}

    public static TrainingGoal toDomain(TrainingGoalJpaEntity t) {
        return TrainingGoal.reconstitute(t.getId(), t.getName(), t.getDetail(), t.getIconId());
    }

    public static TrainingGoalJpaEntity toEntity(TrainingGoal t) {
        return TrainingGoalJpaEntity.builder()
                .id(t.getId())
                .name(t.getName())
                .detail(t.getDetail())
                .iconId(t.getIconId())
                .build();
    }
}