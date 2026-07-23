package com.alessandropesole.bonwoapp.routine.application.dto;

import com.alessandropesole.bonwoapp.routine.domain.model.SetType;
import com.alessandropesole.bonwoapp.routine.domain.model.WeightMode;

import java.time.Duration;

public record SetConfigResponse(
        SetType type,
        int reps,
        Double weightKg,
        WeightMode weightMode,
        Double totalWeightKg,
        Duration duration
) {}
