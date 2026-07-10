package com.alessandropesole.bonwoapp.catalog.infrastructure.persistence.repository;

import com.alessandropesole.bonwoapp.catalog.domain.model.Equipment;
import com.alessandropesole.bonwoapp.catalog.domain.port.out.EquipmentRepository;
import com.alessandropesole.bonwoapp.catalog.infrastructure.persistence.mapper.EquipmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class EquipmentRepositoryAdapter implements EquipmentRepository {

    private final EquipmentJpaRepository equipmentJpa;

    @Override
    public Equipment save(Equipment e) {
        return EquipmentMapper.toDomain(equipmentJpa.save(EquipmentMapper.toEntity(e)));
    }

    @Override
    public Optional<Equipment> findById(Long id) {
        return equipmentJpa.findById(id).map(EquipmentMapper::toDomain);
    }

    @Override
    public List<Equipment> findAll() {
        return equipmentJpa.findAll().stream().map(EquipmentMapper::toDomain).toList();
    }

    @Override
    public List<Equipment> findAllById(Set<Long> ids) {
        return equipmentJpa.findAllById(ids).stream().map(EquipmentMapper::toDomain).toList();
    }

    @Override
    public boolean existsByName(String name) {
        return equipmentJpa.existsByName(name);
    }

    @Override
    public void deleteById(Long id) {
        equipmentJpa.deleteById(id);
    }

}
