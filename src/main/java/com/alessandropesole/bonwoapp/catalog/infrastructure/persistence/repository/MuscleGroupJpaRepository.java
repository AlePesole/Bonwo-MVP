package com.alessandropesole.bonwoapp.catalog.infrastructure.persistence.repository;

import com.alessandropesole.bonwoapp.catalog.infrastructure.persistence.entity.MuscleGroupJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MuscleGroupJpaRepository
        extends JpaRepository<MuscleGroupJpaEntity, Long> {

    boolean existsByName(String name);
}
