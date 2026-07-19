package com.alessandropesole.bonwoapp.catalog.infrastructure.persistence.mapper;

import com.alessandropesole.bonwoapp.catalog.domain.model.MuscleGroup;
import com.alessandropesole.bonwoapp.catalog.infrastructure.persistence.entity.MuscleGroupJpaEntity;

public final class MuscleGroupPersistenceMapper {

    private MuscleGroupPersistenceMapper() {}

    public static MuscleGroup toDomain(MuscleGroupJpaEntity e) {
        return MuscleGroup.reconstitute(e.getId(), e.getName(), e.getIconId());
    }

    public static MuscleGroupJpaEntity toEntity(MuscleGroup g) {
        return MuscleGroupJpaEntity.builder()
                .id(g.getId()).name(g.getName()).iconId(g.getIconId()).build();
    }
}
