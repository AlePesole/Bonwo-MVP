package com.alessandropesole.bonwoapp.exercise.domain.port.in;

import com.alessandropesole.bonwoapp.exercise.application.dto.CreateExerciseRequest;
import com.alessandropesole.bonwoapp.exercise.application.dto.ExerciseResponse;
import com.alessandropesole.bonwoapp.exercise.application.dto.UpdateExerciseRequest;
import com.alessandropesole.bonwoapp.exercise.domain.model.ExerciseFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ExerciseUseCase {
    ExerciseResponse create(CreateExerciseRequest request, Long ownerId);

    ExerciseResponse getById(Long id, Long ownerId);

    ExerciseResponse update(Long id, UpdateExerciseRequest request, Long ownerId);

    void delete(Long id, Long ownerId);

    Page<ExerciseResponse> listMine(Long ownerId, ExerciseFilter filter, Pageable pageable);
}
