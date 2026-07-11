package com.alessandropesole.bonwoapp.catalog.infrastructure.persistence.mapper;

import com.alessandropesole.bonwoapp.catalog.domain.model.Equipment;
import com.alessandropesole.bonwoapp.catalog.infrastructure.persistence.entity.EquipmentJpaEntity;

public final class EquipmentMapper {

    private EquipmentMapper() {}

    public static Equipment toDomain(EquipmentJpaEntity e) {
        return Equipment.reconstitute(e.getId(), e.getName(), e.getIconId());
    }

    public static EquipmentJpaEntity toEntity(Equipment e) {
        return EquipmentJpaEntity.builder()
                .id(e.getId())
                .name(e.getName())
                .iconId(e.getIconId())
                .build();
    }
}
