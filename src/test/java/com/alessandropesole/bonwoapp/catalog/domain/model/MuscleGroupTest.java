package com.alessandropesole.bonwoapp.catalog.domain.model;

import com.alessandropesole.bonwoapp.catalog.domain.exception.InvalidCatalogNameException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MuscleGroupTest {

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "  ", "ab"})
    void create_rejectsBlankOrTooShortName(String invalidName) {
        assertThatThrownBy(() -> MuscleGroup.create(invalidName, 1L))
                .isInstanceOf(InvalidCatalogNameException.class);
    }

    @Test
    void create_rejectsNameLongerThan100Characters() {
        String tooLong = "a".repeat(101);

        assertThatThrownBy(() -> MuscleGroup.create(tooLong, 1L))
                .isInstanceOf(InvalidCatalogNameException.class);
    }

    @Test
    void create_rejectsNullIconId() {
        assertThatThrownBy(() -> MuscleGroup.create("Chest", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("iconId");
    }

    @Test
    void reconstitute_setsAllFieldsIncludingId() {
        MuscleGroup group = MuscleGroup.reconstitute(5L, "Back", 2L);

        assertThat(group.getId()).isEqualTo(5L);
        assertThat(group.getName()).isEqualTo("Back");
        assertThat(group.getIconId()).isEqualTo(2L);
    }

    @Test
    void update_onlyOverwritesNonNullFields() {
        MuscleGroup group = MuscleGroup.reconstitute(1L, "Chest", 1L);

        group.update(null, 9L);

        assertThat(group.getName()).isEqualTo("Chest");
        assertThat(group.getIconId()).isEqualTo(9L);
    }

    @Test
    void update_trimsAndValidatesNewName() {
        MuscleGroup group = MuscleGroup.reconstitute(1L, "Chest", 1L);

        group.update("  Shoulders  ", null);

        assertThat(group.getName()).isEqualTo("Shoulders");
        assertThat(group.getIconId()).isEqualTo(1L);
    }

    @Test
    void update_rejectsInvalidNewName() {
        MuscleGroup group = MuscleGroup.reconstitute(1L, "Chest", 1L);

        assertThatThrownBy(() -> group.update("ab", null))
                .isInstanceOf(InvalidCatalogNameException.class);
    }
}