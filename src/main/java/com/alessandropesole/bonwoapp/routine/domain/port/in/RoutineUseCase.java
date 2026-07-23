package com.alessandropesole.bonwoapp.routine.domain.port.in;

import com.alessandropesole.bonwoapp.routine.application.dto.CreateRoutineRequest;
import com.alessandropesole.bonwoapp.routine.application.dto.RoutineResponse;
import com.alessandropesole.bonwoapp.routine.application.dto.UpdateRoutineRequest;
import com.alessandropesole.bonwoapp.routine.domain.model.RoutineFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RoutineUseCase {
    RoutineResponse create(CreateRoutineRequest request, Long ownerId);

    RoutineResponse getById(Long id, Long ownerId);

    RoutineResponse update(Long id, UpdateRoutineRequest request, Long ownerId);

    void delete(Long id, Long ownerId);

    Page<RoutineResponse> listMine(Long ownerId, RoutineFilter filter, Pageable pageable);
}
