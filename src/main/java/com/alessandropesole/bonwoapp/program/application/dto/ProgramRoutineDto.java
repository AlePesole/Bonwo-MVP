package com.alessandropesole.bonwoapp.program.application.dto;

import com.alessandropesole.bonwoapp.exercise.domain.model.Level;
import com.alessandropesole.bonwoapp.routine.application.dto.ExerciseSlotDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.Duration;
import java.util.List;
import java.util.Set;

public record ProgramRoutineDto(
        Long id,
        @NotBlank @Size(max = 200) String title,
        String description,
        Level level,
        String thumbnailUploadToken,
        Long thumbnailId,
        boolean removeThumbnail,
        @Min(1) int position,
        @NotEmpty @Valid List<ExerciseSlotDto> slots,
        Duration restBetweenExercises,
        Set<Long> equipmentIds,
        Set<Long> activityIds,
        Set<Long> trainingGoalIds
) {}
