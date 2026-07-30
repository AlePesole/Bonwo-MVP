package com.alessandropesole.bonwoapp.program.domain.model;

import com.alessandropesole.bonwoapp.exercise.domain.model.Level;
import com.alessandropesole.bonwoapp.exercise.domain.model.MuscleSummary;
import com.alessandropesole.bonwoapp.program.domain.exception.InvalidTrainingProgramException;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TrainingProgramTest {

    @Test
    void create_setsFieldsAndDefaultsLevelToIntermediate() {
        TrainingProgram program = TrainingProgram.create(1L, "  8 Week Strength  ", "desc", null, 10L,
                3, Set.of(1L), Set.of(2L), Set.of(3L));

        assertThat(program.getTitle()).isEqualTo("8 Week Strength");
        assertThat(program.getOwnerId()).isEqualTo(1L);
        assertThat(program.getLevel()).isEqualTo(Level.INTERMEDIATE);
        assertThat(program.getThumbnailId()).isEqualTo(10L);
        assertThat(program.getDaysPerWeek()).isEqualTo(3);
        assertThat(program.getEquipmentIds()).containsExactly(1L);
        assertThat(program.getMuscleSummary().isEmpty()).isTrue();
        assertThat(program.getId()).isNull();
    }

    @Test
    void create_rejectsNullOwnerId() {
        assertThatThrownBy(() -> TrainingProgram.create(null, "Program", null, null, null,
                3, Set.of(), Set.of(), Set.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ownerId");
    }

    @Test
    void create_rejectsBlankTitle() {
        assertThatThrownBy(() -> TrainingProgram.create(1L, "   ", null, null, null,
                3, Set.of(), Set.of(), Set.of()))
                .isInstanceOf(InvalidTrainingProgramException.class);
    }

    @Test
    void create_rejectsDaysPerWeekOutOfRange() {
        assertThatThrownBy(() -> TrainingProgram.create(1L, "Program", null, null, null,
                0, Set.of(), Set.of(), Set.of()))
                .isInstanceOf(InvalidTrainingProgramException.class);
        assertThatThrownBy(() -> TrainingProgram.create(1L, "Program", null, null, null,
                8, Set.of(), Set.of(), Set.of()))
                .isInstanceOf(InvalidTrainingProgramException.class);
    }

    @Test
    void reconstitute_setsAllFieldsIncludingId() {
        TrainingProgram program = TrainingProgram.reconstitute(5L, 1L, "Squat Program", "desc", Level.ADVANCED,
                null, 4, MuscleSummary.empty(), Set.of(), Set.of(), Set.of(), null);

        assertThat(program.getId()).isEqualTo(5L);
        assertThat(program.getTitle()).isEqualTo("Squat Program");
        assertThat(program.getLevel()).isEqualTo(Level.ADVANCED);
        assertThat(program.getDaysPerWeek()).isEqualTo(4);
    }

    @Test
    void update_onlyOverwritesNonNullFields() {
        TrainingProgram program = TrainingProgram.reconstitute(1L, 1L, "Program", "old desc", Level.INTERMEDIATE,
                null, 3, MuscleSummary.empty(), Set.of(), Set.of(), Set.of(), null);

        program.update(null, "new desc", null, null, false, null, null, null, null);

        assertThat(program.getTitle()).isEqualTo("Program");
        assertThat(program.getDescription()).isEqualTo("new desc");
        assertThat(program.getLevel()).isEqualTo(Level.INTERMEDIATE);
    }

    @Test
    void update_removesThumbnailWhenFlagIsTrue() {
        TrainingProgram program = TrainingProgram.reconstitute(1L, 1L, "Program", null, Level.INTERMEDIATE,
                10L, 3, MuscleSummary.empty(), Set.of(), Set.of(), Set.of(), null);

        program.update(null, null, null, null, true, null, null, null, null);

        assertThat(program.getThumbnailId()).isNull();
    }

    @Test
    void update_rejectsDaysPerWeekOutOfRange() {
        TrainingProgram program = TrainingProgram.reconstitute(1L, 1L, "Program", null, Level.INTERMEDIATE,
                null, 3, MuscleSummary.empty(), Set.of(), Set.of(), Set.of(), null);

        assertThatThrownBy(() -> program.update(null, null, null, null, false, 8, null, null, null))
                .isInstanceOf(InvalidTrainingProgramException.class);
    }

    @Test
    void applyMuscleSummary_overwritesSummary() {
        TrainingProgram program = TrainingProgram.reconstitute(1L, 1L, "Program", null, Level.INTERMEDIATE,
                null, 3, MuscleSummary.empty(), Set.of(), Set.of(), Set.of(), null);

        program.applyMuscleSummary(MuscleSummary.of(Map.of(1L, 0.8)));

        assertThat(program.getMuscleSummary().getScore(1L)).isEqualTo(0.8);
    }

    @Test
    void isOwnedBy_trueOnlyForMatchingOwnerId() {
        TrainingProgram program = TrainingProgram.reconstitute(1L, 42L, "Program", null, Level.INTERMEDIATE,
                null, 3, MuscleSummary.empty(), Set.of(), Set.of(), Set.of(), null);

        assertThat(program.isOwnedBy(42L)).isTrue();
        assertThat(program.isOwnedBy(99L)).isFalse();
    }
}
