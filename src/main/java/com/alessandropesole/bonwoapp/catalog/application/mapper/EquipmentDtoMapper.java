package com.alessandropesole.bonwoapp.catalog.application.mapper;

import com.alessandropesole.bonwoapp.catalog.application.dto.EquipmentResponse;
import com.alessandropesole.bonwoapp.catalog.domain.model.Equipment;
import com.alessandropesole.bonwoapp.media.application.dto.ImageResponse;

public final class EquipmentDtoMapper {
    private EquipmentDtoMapper() {}

    public static EquipmentResponse toResponse(Equipment e, ImageResponse icon) {
        return new EquipmentResponse(e.getId(), e.getName(), icon);
    }
}
