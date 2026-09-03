package com.alessandropesole.bonwoapp.exercise.domain.model;

import com.alessandropesole.bonwoapp.exercise.domain.exception.InvalidExerciseTitleException;
import com.alessandropesole.bonwoapp.exercise.domain.exception.InvalidMuscleEntryException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExerciseTest {

    @Test
    void create_setsFieldsAndDefaultsLevelToIntermediate() {
        Exercise exercise = Exercise.create(1L, "  Bench Press  ", null,
                10L, 20L, "desc", "instructions",
                List.of(MuscleEntry.of(4L, 0.7)), null,
                Set.of(1L), Set.of(2L), Set.of(3L));

        assertThat(exercise.getTitle()).isEqualTo("Bench Press");
        assertThat(exercise.getOwnerId()).isEqualTo(1L);
        assertThat(exercise.getLevel()).isEqualTo(Level.INTERMEDIATE);
        assertThat(exercise.getThumbnailId()).isEqualTo(10L);
        assertThat(exercise.getMainVideoId()).isEqualTo(20L);
        assertThat(exercise.getMuscles()).hasSize(1);
        assertThat(exercise.getEquipmentIds()).containsExactly(1L);
        assertThat(exercise.getId()).isNull();
    }

    @Test
    void create_rejectsNullOwnerId() {
        assertThatThrownBy(() -> Exercise.create(null, "Bench Press", null,
                null, null, null, null, List.of(), null, Set.of(), Set.of(), Set.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ownerId");
    }

    @Test
    void create_rejectsBlankTitle() {
        assertThatThrownBy(() -> Exercise.create(1L, "   ", null,
                null, null, null, null, List.of(), null, Set.of(), Set.of(), Set.of()))
                .isInstanceOf(InvalidExerciseTitleException.class);
    }

    @Test
    void create_rejectsDuplicateMuscleSubGroups() {
        List<MuscleEntry> muscles = List.of(MuscleEntry.of(4L, 0.5), MuscleEntry.of(4L, 0.8));

        assertThatThrownBy(() -> Exercise.create(1L, "Bench Press", null,
                null, null, null, null, muscles, null, Set.of(), Set.of(), Set.of()))
                .isInstanceOf(InvalidMuscleEntryException.class);
    }

    @Test
    void reconstitute_setsAllFieldsIncludingId() {
        Exercise exercise = Exercise.reconstitute(5L, 1L, "Squat", Level.ADVANCED,
                null, null, "desc", "steps", List.of(), null, Set.of(), Set.of(), Set.of(), null, null);

        assertThat(exercise.getId()).isEqualTo(5L);
        assertThat(exercise.getTitle()).isEqualTo("Squat");
        assertThat(exercise.getLevel()).isEqualTo(Level.ADVANCED);
    }

    @Test
    void update_onlyOverwritesNonNullFields() {
        Exercise exercise = Exercise.reconstitute(1L, 1L, "Bench Press", Level.INTERMEDIATE,
                null, null, "old desc", "old steps", List.of(), null, Set.of(), Set.of(), Set.of(), null, null);

        exercise.update(null, null, null, false, null, false,
                "new desc", null, null, null, null, null, null);

        assertThat(exercise.getTitle()).isEqualTo("Bench Press");
        assertThat(exercise.getDescription()).isEqualTo("new desc");
        assertThat(exercise.getInstructions()).isEqualTo("old steps");
    }

    @Test
    void update_removesThumbnailWhenFlagIsTrue() {
        Exercise exercise = Exercise.reconstitute(1L, 1L, "Bench Press", Level.INTERMEDIATE,
                10L, null, null, null, List.of(), null, Set.of(), Set.of(), Set.of(), null, null);

        exercise.update(null, null, null, true, null, false,
                null, null, null, null, null, null, null);

        assertThat(exercise.getThumbnailId()).isNull();
    }

    @Test
    void update_removesMainVideoWhenFlagIsTrue() {
        Exercise exercise = Exercise.reconstitute(1L, 1L, "Bench Press", Level.INTERMEDIATE,
                null, 20L, null, null, List.of(), null, Set.of(), Set.of(), Set.of(), null, null);

        exercise.update(null, null, null, false, null, true,
                null, null, null, null, null, null, null);

        assertThat(exercise.getMainVideoId()).isNull();
    }

    @Test
    void update_rejectsDuplicateMuscleSubGroups() {
        Exercise exercise = Exercise.reconstitute(1L, 1L, "Bench Press", Level.INTERMEDIATE,
                null, null, null, null, List.of(), null, Set.of(), Set.of(), Set.of(), null, null);
        List<MuscleEntry> duplicated = List.of(MuscleEntry.of(4L, 0.5), MuscleEntry.of(4L, 0.9));

        assertThatThrownBy(() -> exercise.update(null, null, null, false, null, false,
                null, null, duplicated, null, null, null, null))
                .isInstanceOf(InvalidMuscleEntryException.class);
    }

    @Test
    void isOwnedBy_trueOnlyForMatchingOwnerId() {
        Exercise exercise = Exercise.reconstitute(1L, 42L, "Bench Press", Level.INTERMEDIATE,
                null, null, null, null, List.of(), null, Set.of(), Set.of(), Set.of(), null, null);

        assertThat(exercise.isOwnedBy(42L)).isTrue();
        assertThat(exercise.isOwnedBy(99L)).isFalse();
    }
}
