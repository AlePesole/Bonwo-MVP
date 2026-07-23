package com.alessandropesole.bonwoapp.routine.infrastructure.persistence.repository;

import com.alessandropesole.bonwoapp.routine.domain.model.Routine;
import com.alessandropesole.bonwoapp.routine.domain.port.out.RoutineRepository;
import com.alessandropesole.bonwoapp.routine.infrastructure.persistence.mapper.RoutineMapper;
import com.alessandropesole.bonwoapp.routine.infrastructure.persistence.specification.RoutineSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RoutineRepositoryAdapter implements RoutineRepository {

    private final RoutineJpaRepository jpa;

    @Override
    public Routine save(Routine r) {
        return RoutineMapper.toDomain(jpa.save(RoutineMapper.toEntity(r)));
    }

    @Override
    public Optional<Routine> findById(Long id) {
        return jpa.findById(id).map(RoutineMapper::toDomain);
    }

    @Override
    public Page<Routine> findByOwner(Long ownerId, Set<Long> equipmentIds, Set<Long> activityIds,
                                     Set<Long> trainingGoalIds, Pageable pageable) {
        var spec = RoutineSpecifications.matching(ownerId, equipmentIds, activityIds, trainingGoalIds);
        return jpa.findAll(spec, pageable).map(RoutineMapper::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }
}
