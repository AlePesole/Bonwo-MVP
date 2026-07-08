package com.alessandropesole.bonwoapp.catalog.domain.port.out;

import com.alessandropesole.bonwoapp.catalog.domain.model.Equipment;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface EquipmentRepository {
    Equipment save(Equipment equipment);

    Optional<Equipment> findById(Long id);

    List<Equipment> findAll();

    List<Equipment> findAllById(Set<Long> ids);

    boolean existsByName(String name);

    void deleteById(Long id);
}
