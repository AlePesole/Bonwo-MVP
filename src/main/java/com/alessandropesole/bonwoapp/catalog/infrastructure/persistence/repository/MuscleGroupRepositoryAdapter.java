package com.alessandropesole.bonwoapp.catalog.infrastructure.persistence.repository;

import com.alessandropesole.bonwoapp.catalog.domain.model.MuscleGroup;
import com.alessandropesole.bonwoapp.catalog.domain.port.out.MuscleGroupRepository;
import com.alessandropesole.bonwoapp.catalog.infrastructure.persistence.mapper.MuscleGroupPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MuscleGroupRepositoryAdapter implements MuscleGroupRepository {

    private final MuscleGroupJpaRepository jpa;

    @Override
    public MuscleGroup save(MuscleGroup g) {
        return MuscleGroupPersistenceMapper.toDomain(jpa.save(MuscleGroupPersistenceMapper.toEntity(g)));
    }

    @Override
    public Optional<MuscleGroup> findById(Long id) {
        return jpa.findById(id).map(MuscleGroupPersistenceMapper::toDomain);
    }

    @Override
    public List<MuscleGroup> findAll() {
        return jpa.findAll().stream()
                .map(MuscleGroupPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<MuscleGroup> findAllById(Collection<Long> ids) {
        return jpa.findAllById(ids).stream()
                .map(MuscleGroupPersistenceMapper::toDomain)
                .toList();
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
