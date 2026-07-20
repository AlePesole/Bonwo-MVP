package com.alessandropesole.bonwoapp.exercise.application.dto;

import com.alessandropesole.bonwoapp.exercise.domain.model.Level;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;

public record UpdateExerciseRequest(
        @Size(max = 200) String title,
        Level level,
        String thumbnailUploadToken,
        boolean removeThumbnail,
        String mainVideoUploadToken,
        boolean removeMainVideo,
        String description,
        String instructions,
        @Valid List<MuscleEntryDto> muscles,
        Set<Long> equipmentIds,
        Set<Long> activityIds,
        Set<Long> trainingGoalIds
) {}
