package com.alessandropesole.bonwoapp.catalog.infrastructure.persistence.repository;

import com.alessandropesole.bonwoapp.catalog.infrastructure.persistence.entity.MuscleSubGroupJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MuscleSubGroupJpaRepository extends JpaRepository<MuscleSubGroupJpaEntity, Long> {

    List<MuscleSubGroupJpaEntity> findByGroupId(Long groupId);

    boolean existsByGroupIdAndName(Long groupId, String name);
}
