package com.alessandropesole.bonwoapp.routine.infrastructure.persistence.repository;

import com.alessandropesole.bonwoapp.routine.infrastructure.persistence.entity.RoutineJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface RoutineJpaRepository extends JpaRepository<RoutineJpaEntity, Long>,
        JpaSpecificationExecutor<RoutineJpaEntity> {
    List<RoutineJpaEntity> findByTrainingProgramIdOrderByPositionAsc(Long trainingProgramId);
}
