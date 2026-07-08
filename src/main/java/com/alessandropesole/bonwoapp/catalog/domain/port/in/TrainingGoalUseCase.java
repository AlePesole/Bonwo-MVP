package com.alessandropesole.bonwoapp.catalog.domain.port.in;

import com.alessandropesole.bonwoapp.catalog.application.dto.TrainingGoalRequest;
import com.alessandropesole.bonwoapp.catalog.application.dto.TrainingGoalResponse;

import java.util.List;

public interface TrainingGoalUseCase {
    List<TrainingGoalResponse> listAll();

    TrainingGoalResponse create(TrainingGoalRequest request, Long adminId);

    TrainingGoalResponse update(Long id, TrainingGoalRequest request, Long adminId);

    void delete(Long id);
}
