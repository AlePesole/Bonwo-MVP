package com.alessandropesole.bonwoapp.catalog.domain.model;

import com.alessandropesole.bonwoapp.catalog.domain.exception.InvalidActivityNameException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ActivityTest {

    @Test
    void create_setsFieldsAndTrimsName() {
        Activity activity = Activity.create("  Running  ", "Cardio activity", 10L);

        assertThat(activity.getName()).isEqualTo("Running");
        assertThat(activity.getDetail()).isEqualTo("Cardio activity");
        assertThat(activity.getIconId()).isEqualTo(10L);
        assertThat(activity.getId()).isNull();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void create_rejectsBlankOrNullName(String invalidName) {
        assertThatThrownBy(() -> Activity.create(invalidName, "detail", 1L))
                .isInstanceOf(InvalidActivityNameException.class);
    }

    @Test
    void create_rejectsNameLongerThan100Characters() {
        String tooLong = "a".repeat(101);

        assertThatThrownBy(() -> Activity.create(tooLong, "detail", 1L))
                .isInstanceOf(InvalidActivityNameException.class);
    }

    @Test
    void create_rejectsNullIconId() {
        assertThatThrownBy(() -> Activity.create("Running", "detail", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("iconId");
    }

    @Test
    void reconstitute_setsAllFieldsIncludingId() {
        Activity activity = Activity.reconstitute(5L, "Cycling", "detail", 2L);

        assertThat(activity.getId()).isEqualTo(5L);
        assertThat(activity.getName()).isEqualTo("Cycling");
        assertThat(activity.getDetail()).isEqualTo("detail");
        assertThat(activity.getIconId()).isEqualTo(2L);
    }

    @Test
    void update_onlyOverwritesNonNullFields() {
        Activity activity = Activity.reconstitute(1L, "Running", "Old detail", 1L);

        activity.update(null, "New detail", null);

        assertThat(activity.getName()).isEqualTo("Running");
        assertThat(activity.getDetail()).isEqualTo("New detail");
        assertThat(activity.getIconId()).isEqualTo(1L);
    }

    @Test
    void update_trimsAndValidatesNewName() {
        Activity activity = Activity.reconstitute(1L, "Running", "detail", 1L);

        activity.update("  Swimming  ", null, 9L);

        assertThat(activity.getName()).isEqualTo("Swimming");
        assertThat(activity.getIconId()).isEqualTo(9L);
    }

    @Test
    void update_rejectsInvalidNewName() {
        Activity activity = Activity.reconstitute(1L, "Running", "detail", 1L);

        assertThatThrownBy(() -> activity.update("", null, null))
                .isInstanceOf(InvalidActivityNameException.class);
    }
}