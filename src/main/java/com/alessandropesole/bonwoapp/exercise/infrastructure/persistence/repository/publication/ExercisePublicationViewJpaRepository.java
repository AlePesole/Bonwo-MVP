package com.alessandropesole.bonwoapp.exercise.infrastructure.persistence.repository.publication;

import com.alessandropesole.bonwoapp.exercise.infrastructure.persistence.entity.publication.ExercisePublicationViewJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExercisePublicationViewJpaRepository extends JpaRepository<ExercisePublicationViewJpaEntity, Long> {
    boolean existsByPublicationIdAndUserId(Long publicationId, Long userId);
}
