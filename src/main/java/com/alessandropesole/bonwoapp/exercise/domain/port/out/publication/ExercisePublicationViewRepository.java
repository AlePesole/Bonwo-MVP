package com.alessandropesole.bonwoapp.exercise.domain.port.out.publication;

public interface ExercisePublicationViewRepository {
    boolean exists(Long publicationId, Long userId);

    void add(Long publicationId, Long userId);
}
