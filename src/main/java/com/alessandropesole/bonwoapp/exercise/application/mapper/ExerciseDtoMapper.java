package com.alessandropesole.bonwoapp.exercise.application.mapper;

import com.alessandropesole.bonwoapp.catalog.application.dto.ActivityResponse;
import com.alessandropesole.bonwoapp.catalog.application.dto.EquipmentResponse;
import com.alessandropesole.bonwoapp.catalog.application.dto.TrainingGoalResponse;
import com.alessandropesole.bonwoapp.exercise.application.dto.ExerciseResponse;
import com.alessandropesole.bonwoapp.exercise.application.dto.MuscleEntryResponse;
import com.alessandropesole.bonwoapp.exercise.domain.model.Exercise;
import com.alessandropesole.bonwoapp.media.application.dto.ImageResponse;
import com.alessandropesole.bonwoapp.media.application.dto.VideoResponse;

import java.util.List;
import java.util.Map;

public final class ExerciseDtoMapper {

    private ExerciseDtoMapper() {
    }

    public static ExerciseResponse toResponse(Exercise e,
                                              List<EquipmentResponse> equipment,
                                              List<ActivityResponse> activities,
                                              List<TrainingGoalResponse> trainingGoals,
                                              ImageResponse thumbnail,
                                              VideoResponse mainVideo,
                                              List<MuscleEntryResponse> muscles) {
        return new ExerciseResponse(
                e.getId(), e.getOwnerId(), e.getTitle(), e.getLevel(),
                thumbnail, mainVideo,
                e.getDescription(), e.getInstructions(),
                e.getMuscleSummary().getScores(),
                muscles,
                equipment, activities, trainingGoals,
                e.getCreatedAt(),
                e.getPublicationId()
        );
    }
}
