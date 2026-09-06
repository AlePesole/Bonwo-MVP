package com.alessandropesole.bonwoapp.exercise.domain.port.in;

import com.alessandropesole.bonwoapp.exercise.application.dto.CreateExerciseRequest;
import com.alessandropesole.bonwoapp.exercise.application.dto.ExerciseResponse;
import com.alessandropesole.bonwoapp.exercise.application.dto.UpdateExerciseRequest;
import com.alessandropesole.bonwoapp.exercise.domain.model.ExerciseFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.Set;

public interface ExerciseUseCase {
    ExerciseResponse create(CreateExerciseRequest request, Long ownerId);

    ExerciseResponse getById(Long id, Long ownerId);

    /** Batched version of {@link #getById} for resolving many exercises (e.g. a routine's or
     *  training session's slots) in one pass. Ids that don't exist or aren't visible to the
     *  viewer are simply absent from the result map — never throws. */
    Map<Long, ExerciseResponse> getVisibleByIds(Set<Long> ids, Long viewerId);

    ExerciseResponse update(Long id, UpdateExerciseRequest request, Long ownerId);

    void delete(Long id, Long ownerId);

    Page<ExerciseResponse> listMine(Long ownerId, ExerciseFilter filter, Pageable pageable);
}
