package com.alessandropesole.bonwoapp.exercise.domain.port.out.publication;

public interface ExercisePublicationUseRepository {
    boolean exists(Long publicationId, Long userId);

    void add(Long publicationId, Long userId, Long routineId);
}
