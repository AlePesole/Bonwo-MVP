package com.alessandropesole.bonwoapp.exercise.application.dto;

import com.alessandropesole.bonwoapp.catalog.application.dto.MuscleSubGroupResponse;
import com.alessandropesole.bonwoapp.exercise.domain.model.ActivationLevel;

public record MuscleEntryResponse(
        Long subGroupId,
        MuscleSubGroupResponse subGroup,
        double activation,
        ActivationLevel role
) {}
