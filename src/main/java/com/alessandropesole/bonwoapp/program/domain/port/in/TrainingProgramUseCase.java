package com.alessandropesole.bonwoapp.program.domain.port.in;

import com.alessandropesole.bonwoapp.program.application.dto.CreateTrainingProgramRequest;
import com.alessandropesole.bonwoapp.program.application.dto.TrainingProgramResponse;
import com.alessandropesole.bonwoapp.program.application.dto.UpdateTrainingProgramRequest;
import com.alessandropesole.bonwoapp.program.domain.model.TrainingProgramFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TrainingProgramUseCase {
    TrainingProgramResponse create(CreateTrainingProgramRequest request, Long ownerId);

    TrainingProgramResponse getById(Long id, Long ownerId);

    TrainingProgramResponse update(Long id, UpdateTrainingProgramRequest request, Long ownerId);

    void delete(Long id, Long ownerId);

    Page<TrainingProgramResponse> listMine(Long ownerId, TrainingProgramFilter filter, Pageable pageable);
}
