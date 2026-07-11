package com.alessandropesole.bonwoapp.catalog.infrastructure.persistence.repository;

import com.alessandropesole.bonwoapp.catalog.infrastructure.persistence.entity.TrainingGoalJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrainingGoalJpaRepository
        extends JpaRepository<TrainingGoalJpaEntity, Long> {
    boolean existsByName(String name);
}
