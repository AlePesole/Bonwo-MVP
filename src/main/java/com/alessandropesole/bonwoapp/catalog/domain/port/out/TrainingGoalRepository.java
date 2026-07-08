package com.alessandropesole.bonwoapp.catalog.domain.port.out;

import com.alessandropesole.bonwoapp.catalog.domain.model.TrainingGoal;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TrainingGoalRepository {
    TrainingGoal save(TrainingGoal trainingGoal);

    Optional<TrainingGoal> findById(Long id);

    List<TrainingGoal> findAll();

    List<TrainingGoal> findAllById(Collection<Long> ids);

    boolean existsByName(String name);

    void deleteById(Long id);
}
