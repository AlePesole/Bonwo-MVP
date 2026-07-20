package com.alessandropesole.bonwoapp.exercise.application.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record MuscleEntryDto(
        @NotNull Long subGroupId,
        @NotNull @DecimalMin("0.1") @DecimalMax("1.0") Double activation
) {}
