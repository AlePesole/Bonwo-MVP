package com.alessandropesole.bonwoapp.exercise.infrastructure.persistence.repository.publication;

import com.alessandropesole.bonwoapp.exercise.infrastructure.persistence.entity.publication.ExercisePublicationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ExercisePublicationJpaRepository extends JpaRepository<ExercisePublicationJpaEntity, Long>,
        JpaSpecificationExecutor<ExercisePublicationJpaEntity> {
    Optional<ExercisePublicationJpaEntity> findByExerciseId(Long exerciseId);
}
