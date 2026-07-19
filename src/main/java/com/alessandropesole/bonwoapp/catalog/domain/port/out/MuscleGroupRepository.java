package com.alessandropesole.bonwoapp.catalog.domain.port.out;

import com.alessandropesole.bonwoapp.catalog.domain.model.MuscleGroup;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MuscleGroupRepository {
    MuscleGroup save(MuscleGroup group);

    Optional<MuscleGroup> findById(Long id);

    List<MuscleGroup> findAll();

    List<MuscleGroup> findAllById(Collection<Long> ids);

    boolean existsByName(String name);

    void deleteById(Long id);
}
