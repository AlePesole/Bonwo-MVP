package com.alessandropesole.bonwoapp.program.application.mapper;

import com.alessandropesole.bonwoapp.catalog.application.dto.ActivityResponse;
import com.alessandropesole.bonwoapp.catalog.application.dto.EquipmentResponse;
import com.alessandropesole.bonwoapp.catalog.application.dto.TrainingGoalResponse;
import com.alessandropesole.bonwoapp.media.application.dto.ImageResponse;
import com.alessandropesole.bonwoapp.program.application.dto.TrainingProgramResponse;
import com.alessandropesole.bonwoapp.program.domain.model.TrainingProgram;
import com.alessandropesole.bonwoapp.routine.application.dto.RoutineResponse;

import java.util.List;

public final class TrainingProgramDtoMapper {

    private TrainingProgramDtoMapper() {
    }

    public static TrainingProgramResponse toResponse(TrainingProgram p,
                                                      List<EquipmentResponse> equipment,
                                                      List<ActivityResponse> activities,
                                                      List<TrainingGoalResponse> trainingGoals,
                                                      ImageResponse thumbnail,
                                                      List<RoutineResponse> routines) {
        return new TrainingProgramResponse(
                p.getId(), p.getOwnerId(), p.getTitle(), p.getDescription(), p.getLevel(),
                thumbnail, p.getDaysPerWeek(), routines,
                p.getMuscleSummary().getScores(),
                equipment, activities, trainingGoals,
                p.getCreatedAt()
        );
    }
}
