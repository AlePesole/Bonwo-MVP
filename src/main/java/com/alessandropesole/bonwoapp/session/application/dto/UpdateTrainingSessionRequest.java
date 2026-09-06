package com.alessandropesole.bonwoapp.session.application.dto;

import jakarta.validation.Valid;

import java.util.List;

public record UpdateTrainingSessionRequest(
        @Valid List<TrainingSlotDto> slots,
        String finalNote
) {}
