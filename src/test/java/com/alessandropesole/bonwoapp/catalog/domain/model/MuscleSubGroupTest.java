package com.alessandropesole.bonwoapp.catalog.domain.model;

import com.alessandropesole.bonwoapp.catalog.domain.exception.InvalidCatalogNameException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MuscleSubGroupTest {

    @Test
    void create_setsFieldsAndTrimsNameAndSvgPathIds() {
        MuscleSubGroup subGroup = MuscleSubGroup.create(1L, "  Upper Chest  ", "detail",
                "  front-1  ", "  back-1  ", 10L);

        assertThat(subGroup.getGroupId()).isEqualTo(1L);
        assertThat(subGroup.getName()).isEqualTo("Upper Chest");
        assertThat(subGroup.getDetail()).isEqualTo("detail");
        assertThat(subGroup.getSvgPathFront()).isEqualTo("front-1");
        assertThat(subGroup.getSvgPathBack()).isEqualTo("back-1");
        assertThat(subGroup.getIconId()).isEqualTo(10L);
        assertThat(subGroup.getId()).isNull();
    }

    @Test
    void create_allowsFrontOnly() {
        MuscleSubGroup subGroup = MuscleSubGroup.create(1L, "Pecs", null, "front-1", null, null);

        assertThat(subGroup.getSvgPathFront()).isEqualTo("front-1");
        assertThat(subGroup.getSvgPathBack()).isNull();
    }

    @Test
    void create_allowsBackOnly() {
        MuscleSubGroup subGroup = MuscleSubGroup.create(1L, "Lats", null, null, "back-1", null);

        assertThat(subGroup.getSvgPathFront()).isNull();
        assertThat(subGroup.getSvgPathBack()).isEqualTo("back-1");
    }

    @Test
    void create_rejectsNullGroupId() {
        assertThatThrownBy(() -> MuscleSubGroup.create(null, "Upper Chest", null, "front-1", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("groupId");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "  ", "ab"})
    void create_rejectsBlankOrTooShortName(String invalidName) {
        assertThatThrownBy(() -> MuscleSubGroup.create(1L, invalidName, null, "front-1", null, null))
                .isInstanceOf(InvalidCatalogNameException.class);
    }

    @Test
    void create_rejectsWhenBothSvgPathsAreMissing() {
        assertThatThrownBy(() -> MuscleSubGroup.create(1L, "Upper Chest", null, null, null, null))
                .isInstanceOf(InvalidCatalogNameException.class);
    }

    @Test
    void reconstitute_setsAllFieldsIncludingId() {
        MuscleSubGroup subGroup = MuscleSubGroup.reconstitute(5L, 1L, "Lower Chest",
                "detail", "front-2", "back-2", 2L);

        assertThat(subGroup.getId()).isEqualTo(5L);
        assertThat(subGroup.getGroupId()).isEqualTo(1L);
        assertThat(subGroup.getName()).isEqualTo("Lower Chest");
        assertThat(subGroup.getDetail()).isEqualTo("detail");
        assertThat(subGroup.getSvgPathFront()).isEqualTo("front-2");
        assertThat(subGroup.getSvgPathBack()).isEqualTo("back-2");
        assertThat(subGroup.getIconId()).isEqualTo(2L);
    }

    @Test
    void update_onlyOverwritesNonNullFields() {
        MuscleSubGroup subGroup = MuscleSubGroup.reconstitute(1L, 1L, "Upper Chest",
                "old detail", "front-1", "back-1", 1L);

        subGroup.update(null, null, null, null, 9L);

        assertThat(subGroup.getName()).isEqualTo("Upper Chest");
        assertThat(subGroup.getDetail()).isEqualTo("old detail");
        assertThat(subGroup.getSvgPathFront()).isEqualTo("front-1");
        assertThat(subGroup.getSvgPathBack()).isEqualTo("back-1");
        assertThat(subGroup.getIconId()).isEqualTo(9L);
    }

    @Test
    void update_rejectsInvalidNewName() {
        MuscleSubGroup subGroup = MuscleSubGroup.reconstitute(1L, 1L, "Upper Chest",
                null, "front-1", null, 1L);

        assertThatThrownBy(() -> subGroup.update("ab", null, null, null, null))
                .isInstanceOf(InvalidCatalogNameException.class);
    }

    @Test
    void update_rejectsClearingBothSvgPathsAtOnce() {
        MuscleSubGroup subGroup = MuscleSubGroup.reconstitute(1L, 1L, "Upper Chest",
                null, "front-1", "back-1", 1L);

        assertThatThrownBy(() -> subGroup.update(null, null, "  ", "  ", null))
                .isInstanceOf(InvalidCatalogNameException.class);
    }
}
