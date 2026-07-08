package com.alessandropesole.bonwoapp.catalog.domain.port.in;

import com.alessandropesole.bonwoapp.catalog.application.dto.TrainingGoalRequest;
import com.alessandropesole.bonwoapp.catalog.application.dto.TrainingGoalResponse;

import java.util.List;

public interface TrainingGoalUseCase {
    List<TrainingGoalResponse> listAll();

    TrainingGoalResponse create(TrainingGoalRequest request);

    TrainingGoalResponse update(Long id, TrainingGoalRequest request);

    void delete(Long id);
}
