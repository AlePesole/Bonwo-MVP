package com.alessandropesole.bonwoapp.program.application.dto;

import com.alessandropesole.bonwoapp.exercise.domain.model.Level;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Set;

public record CreateTrainingProgramRequest(
        @NotBlank @Size(max = 200) String title,
        String description,
        Level level,
        String thumbnailUploadToken,
        Long thumbnailId,
        @Min(1) @Max(7) int daysPerWeek,
        @Valid List<ProgramRoutineDto> routines,
        Set<Long> equipmentIds,
        Set<Long> activityIds,
        Set<Long> trainingGoalIds
) {}
