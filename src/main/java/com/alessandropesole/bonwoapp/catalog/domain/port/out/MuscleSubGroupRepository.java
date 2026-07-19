package com.alessandropesole.bonwoapp.catalog.domain.port.out;

import com.alessandropesole.bonwoapp.catalog.domain.model.MuscleSubGroup;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MuscleSubGroupRepository {
    MuscleSubGroup save(MuscleSubGroup subGroup);

    Optional<MuscleSubGroup> findById(Long id);

    List<MuscleSubGroup> findAll();

    List<MuscleSubGroup> findAllById(Collection<Long> ids);

    List<MuscleSubGroup> findByGroupId(Long groupId);

    boolean existsByGroupIdAndName(Long groupId, String name);

    void deleteById(Long id);
}
