package com.alessandropesole.bonwoapp.catalog.application.dto;

import com.alessandropesole.bonwoapp.media.application.dto.ImageResponse;

public record ActivityResponse(
        Long id,
        String name,
        String detail,
        ImageResponse icon
) {}
