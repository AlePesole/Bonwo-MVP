package com.alessandropesole.bonwoapp.catalog.infrastructure.persistence.mapper;

import com.alessandropesole.bonwoapp.catalog.domain.model.MuscleSubGroup;
import com.alessandropesole.bonwoapp.catalog.infrastructure.persistence.entity.MuscleSubGroupJpaEntity;

public final class MuscleSubGroupPersistenceMapper {

    private MuscleSubGroupPersistenceMapper() {}

    public static MuscleSubGroup toDomain(MuscleSubGroupJpaEntity e) {
        return MuscleSubGroup.reconstitute(e.getId(), e.getGroupId(), e.getName(), e.getDetail(),
                e.getSvgPathFront(), e.getSvgPathBack(), e.getIconId());
    }

    public static MuscleSubGroupJpaEntity toEntity(MuscleSubGroup s) {
        return MuscleSubGroupJpaEntity.builder()
                .id(s.getId()).groupId(s.getGroupId()).name(s.getName()).detail(s.getDetail())
                .svgPathFront(s.getSvgPathFront()).svgPathBack(s.getSvgPathBack())
                .iconId(s.getIconId())
                .build();
    }
}