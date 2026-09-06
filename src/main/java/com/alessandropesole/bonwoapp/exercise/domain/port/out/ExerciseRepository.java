package com.alessandropesole.bonwoapp.exercise.domain.port.out;

import com.alessandropesole.bonwoapp.exercise.domain.model.Exercise;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ExerciseRepository {
    Exercise save(Exercise exercise);

    Optional<Exercise> findById(Long id);

    List<Exercise> findAllById(Set<Long> ids);

    Page<Exercise> findByOwner(Long ownerId, Set<Long> muscleSubGroupIds, Set<Long> equipmentIds,
                               Set<Long> activityIds, Set<Long> trainingGoalIds, String title, Pageable pageable);

    void deleteById(Long id);
}
