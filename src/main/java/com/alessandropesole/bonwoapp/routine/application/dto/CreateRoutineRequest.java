package com.alessandropesole.bonwoapp.routine.application.dto;

import com.alessandropesole.bonwoapp.exercise.domain.model.Level;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Duration;
import java.util.List;
import java.util.Set;

public record CreateRoutineRequest(
        @NotBlank @Size(max = 200) String title,
        String description,
        Level level,
        String thumbnailUploadToken,
        Long thumbnailId,
        @Valid List<ExerciseSlotDto> slots,
        Duration restBetweenExercises,
        Set<Long> equipmentIds,
        Set<Long> activityIds,
        Set<Long> trainingGoalIds
) {}
