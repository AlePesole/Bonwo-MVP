package com.alessandropesole.bonwoapp.catalog.domain.model;

import com.alessandropesole.bonwoapp.catalog.domain.exception.InvalidEquipmentNameException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EquipmentTest {

    @Test
    void create_setsFieldsAndTrimsName() {
        Equipment equipment = Equipment.create("  Dumbbell  ", 10L);

        assertThat(equipment.getName()).isEqualTo("Dumbbell");
        assertThat(equipment.getIconId()).isEqualTo(10L);
        assertThat(equipment.getId()).isNull();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void create_rejectsBlankOrNullName(String invalidName) {
        assertThatThrownBy(() -> Equipment.create(invalidName, 1L))
                .isInstanceOf(InvalidEquipmentNameException.class);
    }

    @Test
    void create_rejectsNameLongerThan100Characters() {
        String tooLong = "a".repeat(101);

        assertThatThrownBy(() -> Equipment.create(tooLong, 1L))
                .isInstanceOf(InvalidEquipmentNameException.class);
    }

    @Test
    void create_rejectsNullIconId() {
        assertThatThrownBy(() -> Equipment.create("Dumbbell", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("iconId");
    }

    @Test
    void reconstitute_setsAllFieldsIncludingId() {
        Equipment equipment = Equipment.reconstitute(5L, "Kettlebell", 2L);

        assertThat(equipment.getId()).isEqualTo(5L);
        assertThat(equipment.getName()).isEqualTo("Kettlebell");
        assertThat(equipment.getIconId()).isEqualTo(2L);
    }

    @Test
    void update_onlyOverwritesNonNullFields() {
        Equipment equipment = Equipment.reconstitute(1L, "Dumbbell", 1L);

        equipment.update(null, 9L);

        assertThat(equipment.getName()).isEqualTo("Dumbbell");
        assertThat(equipment.getIconId()).isEqualTo(9L);
    }

    @Test
    void update_trimsAndValidatesNewName() {
        Equipment equipment = Equipment.reconstitute(1L, "Dumbbell", 1L);

        equipment.update("  Barbell  ", null);

        assertThat(equipment.getName()).isEqualTo("Barbell");
        assertThat(equipment.getIconId()).isEqualTo(1L);
    }

    @Test
    void update_rejectsInvalidNewName() {
        Equipment equipment = Equipment.reconstitute(1L, "Dumbbell", 1L);

        assertThatThrownBy(() -> equipment.update("", null))
                .isInstanceOf(InvalidEquipmentNameException.class);
    }
}