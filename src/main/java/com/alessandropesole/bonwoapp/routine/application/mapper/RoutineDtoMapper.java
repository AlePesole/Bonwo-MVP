package com.alessandropesole.bonwoapp.routine.application.mapper;

import com.alessandropesole.bonwoapp.catalog.application.dto.ActivityResponse;
import com.alessandropesole.bonwoapp.catalog.application.dto.EquipmentResponse;
import com.alessandropesole.bonwoapp.catalog.application.dto.TrainingGoalResponse;
import com.alessandropesole.bonwoapp.media.application.dto.ImageResponse;
import com.alessandropesole.bonwoapp.routine.application.dto.ExerciseSlotResponse;
import com.alessandropesole.bonwoapp.routine.application.dto.RoutineResponse;
import com.alessandropesole.bonwoapp.routine.domain.model.Routine;

import java.util.List;

public final class RoutineDtoMapper {

    private RoutineDtoMapper() {
    }

    public static RoutineResponse toResponse(Routine r,
                                             List<EquipmentResponse> equipment,
                                             List<ActivityResponse> activities,
                                             List<TrainingGoalResponse> trainingGoals,
                                             ImageResponse thumbnail,
                                             List<ExerciseSlotResponse> slots) {
        return new RoutineResponse(
                r.getId(), r.getOwnerId(), r.getTitle(), r.getDescription(), r.getLevel(),
                thumbnail, r.getEstimatedDuration(), r.getRestBetweenExercises(),
                slots, r.getMuscleSummary().getScores(),
                equipment, activities, trainingGoals,
                r.getCreatedAt()
        );
    }
}
