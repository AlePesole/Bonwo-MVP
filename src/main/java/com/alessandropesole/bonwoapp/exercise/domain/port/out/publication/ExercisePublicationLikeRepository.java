package com.alessandropesole.bonwoapp.exercise.domain.port.out.publication;

public interface ExercisePublicationLikeRepository {
    boolean exists(Long publicationId, Long userId);

    void add(Long publicationId, Long userId);

    void remove(Long publicationId, Long userId);
}
