package com.alessandropesole.bonwoapp.catalog.application.dto;

import com.alessandropesole.bonwoapp.media.application.dto.ImageResponse;

public record EquipmentResponse(
        Long id,
        String name,
        ImageResponse icon
) {}
