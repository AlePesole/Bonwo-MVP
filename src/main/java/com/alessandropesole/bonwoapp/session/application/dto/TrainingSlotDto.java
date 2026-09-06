package com.alessandropesole.bonwoapp.session.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Duration;
import java.util.List;

public record TrainingSlotDto(
        @NotNull Long exerciseId,
        @Min(1) int position,
        @NotEmpty @Valid List<TrainingSetDto> sets,
        Duration restBetweenSets
) {}
