package com.alessandropesole.bonwoapp.catalog.application.dto;

import com.alessandropesole.bonwoapp.media.application.dto.ImageResponse;

public record MuscleSubGroupResponse(
        Long id,
        Long groupId,
        String name,
        String detail,
        String svgPathFront,
        String svgPathBack,
        ImageResponse icon
) {}
