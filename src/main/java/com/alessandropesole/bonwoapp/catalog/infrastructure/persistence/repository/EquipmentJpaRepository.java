package com.alessandropesole.bonwoapp.catalog.infrastructure.persistence.repository;

import com.alessandropesole.bonwoapp.catalog.infrastructure.persistence.entity.EquipmentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipmentJpaRepository extends JpaRepository<EquipmentJpaEntity, Long> {
    boolean existsByName(String name);
}
