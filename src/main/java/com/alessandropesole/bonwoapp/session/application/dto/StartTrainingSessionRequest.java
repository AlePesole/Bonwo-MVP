package com.alessandropesole.bonwoapp.session.application.dto;

import jakarta.validation.constraints.NotNull;

public record StartTrainingSessionRequest(
        @NotNull Long routineId
) {}
