package com.alessandropesole.bonwoapp.exercise.domain.port.out.publication;

import com.alessandropesole.bonwoapp.exercise.domain.model.publication.ExercisePublication;
import com.alessandropesole.bonwoapp.exercise.domain.model.publication.PublicationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.Set;

public interface ExercisePublicationRepository {
    ExercisePublication save(ExercisePublication publication);

    Optional<ExercisePublication> findById(Long id);

    Optional<ExercisePublication> findByExerciseId(Long exerciseId);

    void deleteById(Long id);

    /** Public feed — any author, PUBLIC visibility only. */
    Page<ExercisePublication> findFeed(PublicationType type, Set<Long> muscleSubGroupIds,
                                       Set<Long> equipmentIds, Set<Long> activityIds,
                                       Set<Long> trainingGoalIds, String title, Pageable pageable);

    /** The caller's own publications, regardless of visibility. */
    Page<ExercisePublication> findByAuthor(Long authorId, PublicationType type, Set<Long> muscleSubGroupIds,
                                           Set<Long> equipmentIds, Set<Long> activityIds,
                                           Set<Long> trainingGoalIds, String title, Pageable pageable);
}
