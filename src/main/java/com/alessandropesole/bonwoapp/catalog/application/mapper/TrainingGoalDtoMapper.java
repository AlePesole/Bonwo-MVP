package com.alessandropesole.bonwoapp.catalog.application.mapper;

import com.alessandropesole.bonwoapp.catalog.application.dto.TrainingGoalResponse;
import com.alessandropesole.bonwoapp.catalog.domain.model.TrainingGoal;
import com.alessandropesole.bonwoapp.media.application.dto.ImageResponse;

public final class TrainingGoalDtoMapper {
    private TrainingGoalDtoMapper() {}

    public static TrainingGoalResponse toResponse(TrainingGoal t, ImageResponse icon) {
        return new TrainingGoalResponse(t.getId(), t.getName(), t.getDetail(), icon);
    }
}
