package com.alessandropesole.bonwoapp.exercise.application.dto;

import com.alessandropesole.bonwoapp.catalog.application.dto.ActivityResponse;
import com.alessandropesole.bonwoapp.catalog.application.dto.EquipmentResponse;
import com.alessandropesole.bonwoapp.catalog.application.dto.TrainingGoalResponse;
import com.alessandropesole.bonwoapp.exercise.domain.model.Level;
import com.alessandropesole.bonwoapp.media.application.dto.ImageResponse;
import com.alessandropesole.bonwoapp.media.application.dto.VideoResponse;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ExerciseResponse(
        Long id,
        Long ownerId,
        String title,
        Level level,
        ImageResponse thumbnail,
        VideoResponse mainVideo,
        String description,
        String instructions,
        Map<Long, Double> muscleSummary,
        List<MuscleEntryResponse> muscles,
        List<EquipmentResponse> equipment,
        List<ActivityResponse> activities,
        List<TrainingGoalResponse> trainingGoals,
        Instant createdAt,
        Long publicationId
) {}
