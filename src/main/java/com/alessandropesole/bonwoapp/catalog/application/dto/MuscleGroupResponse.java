package com.alessandropesole.bonwoapp.catalog.application.dto;

import com.alessandropesole.bonwoapp.media.application.dto.ImageResponse;
import java.util.List;

public record MuscleGroupResponse(
        Long id,
        String name,
        ImageResponse icon,
        List<MuscleSubGroupResponse> subGroups
) {}
