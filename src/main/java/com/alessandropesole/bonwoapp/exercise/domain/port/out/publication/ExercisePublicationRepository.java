package com.alessandropesole.bonwoapp.exercise.domain.port.out.publication;

import com.alessandropesole.bonwoapp.exercise.domain.model.publication.ExercisePublication;
import com.alessandropesole.bonwoapp.exercise.domain.model.publication.PublicationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ExercisePublicationRepository {
    ExercisePublication save(ExercisePublication publication);

    Optional<ExercisePublication> findById(Long id);

    List<ExercisePublication> findAllById(Set<Long> ids);

    Optional<ExercisePublication> findByExerciseId(Long exerciseId);

    void deleteById(Long id);

    Page<ExercisePublication> findFeed(PublicationType type, Set<Long> muscleSubGroupIds,
                                       Set<Long> equipmentIds, Set<Long> activityIds,
                                       Set<Long> trainingGoalIds, String title, Pageable pageable);

    Page<ExercisePublication> findByAuthor(Long authorId, PublicationType type, Set<Long> muscleSubGroupIds,
                                           Set<Long> equipmentIds, Set<Long> activityIds,
                                           Set<Long> trainingGoalIds, String title, Pageable pageable);

    Page<ExercisePublication> findLikedByUser(Long userId, PublicationType type, Set<Long> muscleSubGroupIds,
                                              Set<Long> equipmentIds, Set<Long> activityIds,
                                              Set<Long> trainingGoalIds, String title, Pageable pageable);

    Page<ExercisePublication> findSavedByUser(Long userId, PublicationType type, Set<Long> muscleSubGroupIds,
                                              Set<Long> equipmentIds, Set<Long> activityIds,
                                              Set<Long> trainingGoalIds, String title, Pageable pageable);
}
