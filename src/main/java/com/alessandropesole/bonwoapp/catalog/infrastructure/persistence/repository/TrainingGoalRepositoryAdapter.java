package com.alessandropesole.bonwoapp.catalog.infrastructure.persistence.repository;

import com.alessandropesole.bonwoapp.catalog.domain.model.TrainingGoal;
import com.alessandropesole.bonwoapp.catalog.domain.port.out.TrainingGoalRepository;
import com.alessandropesole.bonwoapp.catalog.infrastructure.persistence.mapper.TrainingGoalMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class TrainingGoalRepositoryAdapter implements TrainingGoalRepository {

    private final TrainingGoalJpaRepository jpa;

    @Override
    public TrainingGoal save(TrainingGoal t) {
        return TrainingGoalMapper.toDomain(jpa.save(TrainingGoalMapper.toEntity(t)));
    }

    @Override
    public Optional<TrainingGoal> findById(Long id) {
        return jpa.findById(id).map(TrainingGoalMapper::toDomain);
    }

    @Override
    public List<TrainingGoal> findAll() {
        return jpa.findAll().stream().map(TrainingGoalMapper::toDomain).toList();
    }

    @Override
    public List<TrainingGoal> findAllById(Set<Long> ids) {
        return jpa.findAllById(ids).stream().map(TrainingGoalMapper::toDomain).toList();
    }

    @Override
    public boolean existsByName(String name) {
        return jpa.existsByName(name);
    }

    @Override
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }
}
