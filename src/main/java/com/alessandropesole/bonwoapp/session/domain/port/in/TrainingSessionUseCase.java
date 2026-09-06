package com.alessandropesole.bonwoapp.session.domain.port.in;

import com.alessandropesole.bonwoapp.session.application.dto.CompleteTrainingSessionRequest;
import com.alessandropesole.bonwoapp.session.application.dto.StartTrainingSessionRequest;
import com.alessandropesole.bonwoapp.session.application.dto.TrainingSessionResponse;
import com.alessandropesole.bonwoapp.session.application.dto.UpdateTrainingSessionRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TrainingSessionUseCase {
    TrainingSessionResponse start(StartTrainingSessionRequest request, Long ownerId);

    TrainingSessionResponse getById(Long id, Long ownerId);

    TrainingSessionResponse update(Long id, UpdateTrainingSessionRequest request, Long ownerId);

    TrainingSessionResponse complete(Long id, CompleteTrainingSessionRequest request, Long ownerId);

    void delete(Long id, Long ownerId);

    Page<TrainingSessionResponse> listMine(Long ownerId, Pageable pageable);
}
