package com.alessandropesole.bonwoapp.session.application.dto;

import com.alessandropesole.bonwoapp.routine.domain.model.SetType;
import com.alessandropesole.bonwoapp.routine.domain.model.WeightMode;
import jakarta.validation.constraints.NotNull;

import java.time.Duration;

public record TrainingSetDto(
        @NotNull SetType type,
        int reps,
        Double weightKg,
        WeightMode weightMode,
        Duration duration,
        boolean done
) {}
