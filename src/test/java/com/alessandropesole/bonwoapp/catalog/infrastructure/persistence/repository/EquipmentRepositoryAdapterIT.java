package com.alessandropesole.bonwoapp.catalog.infrastructure.persistence.repository;

import com.alessandropesole.bonwoapp.catalog.domain.model.Equipment;
import com.alessandropesole.bonwoapp.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(EquipmentRepositoryAdapter.class)
class EquipmentRepositoryAdapterIT extends AbstractIntegrationTest {

    @Autowired
    private EquipmentRepositoryAdapter equipmentRepository;

    @Test
    void save_persistsAndReloadsEquipment() {
        Equipment saved = equipmentRepository.save(Equipment.create("Dumbbell", 1L));

        assertThat(saved.getId()).isNotNull();

        Equipment reloaded = equipmentRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("Dumbbell");
        assertThat(reloaded.getIconId()).isEqualTo(1L);
    }

    @Test
    void existsByName_reflectsPersistedState() {
        equipmentRepository.save(Equipment.create("Kettlebell", 1L));

        assertThat(equipmentRepository.existsByName("Kettlebell")).isTrue();
        assertThat(equipmentRepository.existsByName("Unknown")).isFalse();
    }

    @Test
    void save_rejectsDuplicateNameAtDatabaseLevel() {
        equipmentRepository.save(Equipment.create("Barbell", 1L));

        assertThatThrownBy(() ->
                equipmentRepository.save(Equipment.create("Barbell", 2L)));
    }

    @Test
    void findAllById_returnsOnlyMatchingEquipment() {
        Equipment e1 = equipmentRepository.save(Equipment.create("Bench", 1L));
        Equipment e2 = equipmentRepository.save(Equipment.create("Rack", 2L));
        equipmentRepository.save(Equipment.create("Mat", 3L));

        var result = equipmentRepository.findAllById(Set.of(e1.getId(), e2.getId()));

        assertThat(result).hasSize(2)
                .extracting(Equipment::getName)
                .containsExactlyInAnyOrder("Bench", "Rack");
    }

    @Test
    void deleteById_removesEquipment() {
        Equipment saved = equipmentRepository.save(Equipment.create("Jump rope", 1L));

        equipmentRepository.deleteById(saved.getId());

        assertThat(equipmentRepository.findById(saved.getId())).isEmpty();
    }
}