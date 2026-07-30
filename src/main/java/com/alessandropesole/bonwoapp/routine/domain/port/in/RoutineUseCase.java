package com.alessandropesole.bonwoapp.routine.domain.port.in;

import com.alessandropesole.bonwoapp.routine.application.dto.CreateRoutineRequest;
import com.alessandropesole.bonwoapp.routine.application.dto.RoutineResponse;
import com.alessandropesole.bonwoapp.routine.application.dto.UpdateRoutineRequest;
import com.alessandropesole.bonwoapp.routine.domain.model.RoutineFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RoutineUseCase {
    RoutineResponse create(CreateRoutineRequest request, Long ownerId);

    /** Internal-only — used by TrainingProgramService to create a Routine that's part of a program's aggregate. */
    RoutineResponse create(CreateRoutineRequest request, Long ownerId, Long trainingProgramId, Integer position);

    RoutineResponse getById(Long id, Long ownerId);

    RoutineResponse update(Long id, UpdateRoutineRequest request, Long ownerId);

    /** Internal-only — used by TrainingProgramService to reorder a Routine within its program. */
    RoutineResponse update(Long id, UpdateRoutineRequest request, Long ownerId, Integer newPosition);

    void delete(Long id, Long ownerId);

    Page<RoutineResponse> listMine(Long ownerId, RoutineFilter filter, Pageable pageable);
}
