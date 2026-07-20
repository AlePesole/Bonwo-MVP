package com.alessandropesole.bonwoapp.exercise.infrastructure.persistence.repository;

import com.alessandropesole.bonwoapp.exercise.infrastructure.persistence.entity.ExerciseJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ExerciseJpaRepository extends JpaRepository<ExerciseJpaEntity, Long>,
        JpaSpecificationExecutor<ExerciseJpaEntity> {

    @Query("SELECT e FROM ExerciseJpaEntity e WHERE e.id = :id AND e.ownerId = :ownerId")
    Optional<ExerciseJpaEntity> findByIdAndOwnerId(@Param("id") Long id,
                                                   @Param("ownerId") Long ownerId);
}
