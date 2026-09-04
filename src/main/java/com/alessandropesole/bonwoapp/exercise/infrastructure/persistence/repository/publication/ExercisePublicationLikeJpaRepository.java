package com.alessandropesole.bonwoapp.exercise.infrastructure.persistence.repository.publication;

import com.alessandropesole.bonwoapp.exercise.infrastructure.persistence.entity.publication.ExercisePublicationLikeJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface ExercisePublicationLikeJpaRepository extends JpaRepository<ExercisePublicationLikeJpaEntity, Long> {
    boolean existsByPublicationIdAndUserId(Long publicationId, Long userId);

    @Transactional
    void deleteByPublicationIdAndUserId(Long publicationId, Long userId);
}
