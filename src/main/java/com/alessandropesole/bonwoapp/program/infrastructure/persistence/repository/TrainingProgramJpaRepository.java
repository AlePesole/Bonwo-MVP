package com.alessandropesole.bonwoapp.program.infrastructure.persistence.repository;

import com.alessandropesole.bonwoapp.program.infrastructure.persistence.entity.TrainingProgramJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TrainingProgramJpaRepository extends JpaRepository<TrainingProgramJpaEntity, Long>,
        JpaSpecificationExecutor<TrainingProgramJpaEntity> {
}
