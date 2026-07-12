package com.alessandropesole.bonwoapp.catalog.domain.model;

import com.alessandropesole.bonwoapp.catalog.domain.exception.InvalidTrainingGoalNameException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TrainingGoalTest {

    @Test
    void create_setsFieldsAndTrimsName() {
        TrainingGoal goal = TrainingGoal.create("  Weight loss  ", "Lose fat", 10L);

        assertThat(goal.getName()).isEqualTo("Weight loss");
        assertThat(goal.getDetail()).isEqualTo("Lose fat");
        assertThat(goal.getIconId()).isEqualTo(10L);
        assertThat(goal.getId()).isNull();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void create_rejectsBlankOrNullName(String invalidName) {
        assertThatThrownBy(() -> TrainingGoal.create(invalidName, "detail", 1L))
                .isInstanceOf(InvalidTrainingGoalNameException.class);
    }

    @Test
    void create_rejectsNameLongerThan100Characters() {
        String tooLong = "a".repeat(101);

        assertThatThrownBy(() -> TrainingGoal.create(tooLong, "detail", 1L))
                .isInstanceOf(InvalidTrainingGoalNameException.class);
    }

    @Test
    void create_rejectsNullIconId() {
        assertThatThrownBy(() -> TrainingGoal.create("Weight loss", "detail", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("iconId");
    }

    @Test
    void reconstitute_setsAllFieldsIncludingId() {
        TrainingGoal goal = TrainingGoal.reconstitute(5L, "Strength", "detail", 2L);

        assertThat(goal.getId()).isEqualTo(5L);
        assertThat(goal.getName()).isEqualTo("Strength");
        assertThat(goal.getDetail()).isEqualTo("detail");
        assertThat(goal.getIconId()).isEqualTo(2L);
    }

    @Test
    void update_onlyOverwritesNonNullFields() {
        TrainingGoal goal = TrainingGoal.reconstitute(1L, "Strength", "Old detail", 1L);

        goal.update(null, "New detail", null);

        assertThat(goal.getName()).isEqualTo("Strength");
        assertThat(goal.getDetail()).isEqualTo("New detail");
        assertThat(goal.getIconId()).isEqualTo(1L);
    }

    @Test
    void update_trimsAndValidatesNewName() {
        TrainingGoal goal = TrainingGoal.reconstitute(1L, "Strength", "detail", 1L);

        goal.update("  Endurance  ", null, 9L);

        assertThat(goal.getName()).isEqualTo("Endurance");
        assertThat(goal.getIconId()).isEqualTo(9L);
    }

    @Test
    void update_rejectsInvalidNewName() {
        TrainingGoal goal = TrainingGoal.reconstitute(1L, "Strength", "detail", 1L);

        assertThatThrownBy(() -> goal.update("", null, null))
                .isInstanceOf(InvalidTrainingGoalNameException.class);
    }
}