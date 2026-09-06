package com.alessandropesole.bonwoapp.session.application.dto;

import jakarta.validation.Valid;

import java.util.List;

/**
 * Covers everything editable during (or after) a session: adding/removing exercises, editing sets,
 * and toggling done — all through the full `slots` replace — plus the final note. Allowed regardless
 * of session status; there is deliberately no way to reopen a COMPLETED session back to IN_PROGRESS.
 */
public record UpdateTrainingSessionRequest(
        @Valid List<TrainingSlotDto> slots,
        String finalNote
) {}
