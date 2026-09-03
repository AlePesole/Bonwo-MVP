package com.alessandropesole.bonwoapp.exercise.infrastructure.persistence.repository.publication;

import com.alessandropesole.bonwoapp.exercise.infrastructure.persistence.entity.publication.ExercisePublicationUseJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExercisePublicationUseJpaRepository extends JpaRepository<ExercisePublicationUseJpaEntity, Long> {
    boolean existsByPublicationIdAndUserId(Long publicationId, Long userId);
}
