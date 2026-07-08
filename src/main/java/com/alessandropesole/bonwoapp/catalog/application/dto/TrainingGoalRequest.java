package com.alessandropesole.bonwoapp.catalog.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TrainingGoalRequest(
        @NotBlank @Size(max = 100) String name,
        String detail,
        Long iconId
) {}
