package com.alessandropesole.bonwoapp.program.infrastructure.persistence.repository;

import com.alessandropesole.bonwoapp.program.domain.model.TrainingProgram;
import com.alessandropesole.bonwoapp.program.domain.port.out.TrainingProgramRepository;
import com.alessandropesole.bonwoapp.program.infrastructure.persistence.mapper.TrainingProgramMapper;
import com.alessandropesole.bonwoapp.program.infrastructure.persistence.specification.TrainingProgramSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class TrainingProgramRepositoryAdapter implements TrainingProgramRepository {

    private final TrainingProgramJpaRepository jpa;

    @Override
    public TrainingProgram save(TrainingProgram p) {
        return TrainingProgramMapper.toDomain(jpa.save(TrainingProgramMapper.toEntity(p)));
    }

    @Override
    public Optional<TrainingProgram> findById(Long id) {
        return jpa.findById(id).map(TrainingProgramMapper::toDomain);
    }

    @Override
    public Page<TrainingProgram> findByOwner(Long ownerId, Set<Long> equipmentIds, Set<Long> activityIds,
                                             Set<Long> trainingGoalIds, Pageable pageable) {
        var spec = TrainingProgramSpecifications.matching(ownerId, equipmentIds, activityIds, trainingGoalIds);
        return jpa.findAll(spec, pageable).map(TrainingProgramMapper::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }
}
