package com.alessandropesole.bonwoapp.catalog.application.mapper;

import com.alessandropesole.bonwoapp.catalog.application.dto.ActivityResponse;
import com.alessandropesole.bonwoapp.catalog.domain.model.Activity;
import com.alessandropesole.bonwoapp.media.application.dto.ImageResponse;

public final class ActivityDtoMapper {
    private ActivityDtoMapper() {}

    public static ActivityResponse toResponse(Activity a, ImageResponse icon) {
        return new ActivityResponse(a.getId(), a.getName(), a.getDetail(), icon);
    }
}
