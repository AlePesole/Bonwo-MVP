package com.alessandropesole.bonwoapp.routine.domain.port.out;

import com.alessandropesole.bonwoapp.routine.domain.model.Routine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.Set;

public interface RoutineRepository {
    Routine save(Routine routine);

    Optional<Routine> findById(Long id);

    Page<Routine> findByOwner(Long ownerId, Set<Long> equipmentIds, Set<Long> activityIds,
                              Set<Long> trainingGoalIds, Pageable pageable);

    void deleteById(Long id);
}
