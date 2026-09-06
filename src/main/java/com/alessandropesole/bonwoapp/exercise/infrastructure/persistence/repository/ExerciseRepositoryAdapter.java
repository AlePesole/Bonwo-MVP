package com.alessandropesole.bonwoapp.exercise.infrastructure.persistence.repository;

import com.alessandropesole.bonwoapp.exercise.domain.model.Exercise;
import com.alessandropesole.bonwoapp.exercise.domain.port.out.ExerciseRepository;
import com.alessandropesole.bonwoapp.exercise.infrastructure.persistence.mapper.ExerciseMapper;
import com.alessandropesole.bonwoapp.exercise.infrastructure.persistence.specification.ExerciseSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ExerciseRepositoryAdapter implements ExerciseRepository {

    private final ExerciseJpaRepository jpa;

    @Override
    public Exercise save(Exercise e) {
        return ExerciseMapper.toDomain(jpa.save(ExerciseMapper.toEntity(e)));
    }

    @Override
    public Optional<Exercise> findById(Long id) {
        return jpa.findById(id).map(ExerciseMapper::toDomain);
    }

    @Override
    public List<Exercise> findAllById(Set<Long> ids) {
        return jpa.findAllById(ids).stream().map(ExerciseMapper::toDomain).toList();
    }

    @Override
    public Page<Exercise> findByOwner(Long ownerId, Set<Long> muscleSubGroupIds, Set<Long> equipmentIds,
                                      Set<Long> activityIds, Set<Long> trainingGoalIds, String title, Pageable pageable) {
        var spec = ExerciseSpecifications.matching(ownerId, muscleSubGroupIds, equipmentIds,
                activityIds, trainingGoalIds, title);
        return jpa.findAll(spec, pageable).map(ExerciseMapper::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }
}
