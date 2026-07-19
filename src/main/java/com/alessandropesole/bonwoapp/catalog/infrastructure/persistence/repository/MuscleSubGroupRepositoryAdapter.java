package com.alessandropesole.bonwoapp.catalog.infrastructure.persistence.repository;

import com.alessandropesole.bonwoapp.catalog.domain.model.MuscleSubGroup;
import com.alessandropesole.bonwoapp.catalog.domain.port.out.MuscleSubGroupRepository;
import com.alessandropesole.bonwoapp.catalog.infrastructure.persistence.mapper.MuscleSubGroupPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MuscleSubGroupRepositoryAdapter implements MuscleSubGroupRepository {

    private final MuscleSubGroupJpaRepository jpa;

    @Override
    public MuscleSubGroup save(MuscleSubGroup s) {
        return MuscleSubGroupPersistenceMapper.toDomain(
                jpa.save(MuscleSubGroupPersistenceMapper.toEntity(s)));
    }

    @Override
    public Optional<MuscleSubGroup> findById(Long id) {
        return jpa.findById(id).map(MuscleSubGroupPersistenceMapper::toDomain);
    }

    @Override
    public List<MuscleSubGroup> findAll() {
        return jpa.findAll().stream().map(MuscleSubGroupPersistenceMapper::toDomain).toList();
    }

    @Override
    public List<MuscleSubGroup> findAllById(Collection<Long> ids) {
        return jpa.findAllById(ids).stream().map(MuscleSubGroupPersistenceMapper::toDomain).toList();
    }

    @Override
    public List<MuscleSubGroup> findByGroupId(Long groupId) {
        return jpa.findByGroupId(groupId).stream()
                .map(MuscleSubGroupPersistenceMapper::toDomain).toList();
    }

    @Override
    public boolean existsByGroupIdAndName(Long groupId, String name) {
        return jpa.existsByGroupIdAndName(groupId, name);
    }

    @Override
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }
}
