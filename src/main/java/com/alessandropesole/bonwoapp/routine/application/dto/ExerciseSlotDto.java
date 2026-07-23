package com.alessandropesole.bonwoapp.routine.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Duration;
import java.util.List;

public record ExerciseSlotDto(
        @NotNull Long exerciseId,
        @Min(1) int position,
        @NotEmpty @Valid List<SetConfigDto> sets,
        Duration restBetweenSets
) {}
