package com.alessandropesole.bonwoapp.program.domain.port.out;

import com.alessandropesole.bonwoapp.program.domain.model.TrainingProgram;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.Set;

public interface TrainingProgramRepository {
    TrainingProgram save(TrainingProgram program);

    Optional<TrainingProgram> findById(Long id);

    Page<TrainingProgram> findByOwner(Long ownerId, Set<Long> equipmentIds, Set<Long> activityIds,
                                      Set<Long> trainingGoalIds, String title, Pageable pageable);

    void deleteById(Long id);
}
