package com.alessandropesole.bonwoapp.routine.domain.model;

import com.alessandropesole.bonwoapp.exercise.domain.model.Level;
import com.alessandropesole.bonwoapp.exercise.domain.model.MuscleSummary;
import com.alessandropesole.bonwoapp.routine.domain.exception.InvalidRoutineTitleException;
import com.alessandropesole.bonwoapp.routine.domain.exception.InvalidSlotException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoutineTest {

    private static ExerciseSlot slotWithOneRepsSet(long exerciseId, int position, Duration restBetweenSets) {
        return ExerciseSlot.create(exerciseId, position,
                List.of(SetConfig.reps(10, null, null)), restBetweenSets);
    }

    @Test
    void create_setsFieldsAndDefaultsLevelToIntermediate() {
        List<ExerciseSlot> slots = List.of(slotWithOneRepsSet(1L, 1, null));

        Routine routine = Routine.create(1L, "  Push Day  ", "desc", null, 10L,
                slots, null, Set.of(1L), Set.of(2L), Set.of(3L));

        assertThat(routine.getTitle()).isEqualTo("Push Day");
        assertThat(routine.getOwnerId()).isEqualTo(1L);
        assertThat(routine.getLevel()).isEqualTo(Level.INTERMEDIATE);
        assertThat(routine.getThumbnailId()).isEqualTo(10L);
        assertThat(routine.getSlots()).hasSize(1);
        assertThat(routine.getEquipmentIds()).containsExactly(1L);
        assertThat(routine.getMuscleSummary().isEmpty()).isTrue();
        assertThat(routine.getId()).isNull();
    }

    @Test
    void create_rejectsNullOwnerId() {
        assertThatThrownBy(() -> Routine.create(null, "Push Day", null, null, null,
                List.of(), null, Set.of(), Set.of(), Set.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ownerId");
    }

    @Test
    void create_rejectsDuplicateSlotPositions() {
        List<ExerciseSlot> slots = List.of(
                slotWithOneRepsSet(1L, 1, null),
                slotWithOneRepsSet(2L, 1, null));

        assertThatThrownBy(() -> Routine.create(1L, "Push Day", null, null, null,
                slots, null, Set.of(), Set.of(), Set.of()))
                .isInstanceOf(InvalidSlotException.class);
    }

    @Test
    void create_withNoSlotsResultsInZeroDuration() {
        Routine routine = Routine.create(1L, "Push Day", null, null, null,
                List.of(), null, Set.of(), Set.of(), Set.of());

        assertThat(routine.getEstimatedDuration()).isEqualTo(Duration.ZERO);
    }

    @Test
    void create_computesDurationFromSetsAndRestBetweenSets() {

        ExerciseSlot slot = ExerciseSlot.create(1L, 1,
                List.of(SetConfig.reps(10, null, null), SetConfig.reps(10, null, null)),
                Duration.ofSeconds(30));

        Routine routine = Routine.create(1L, "Push Day", null, null, null,
                List.of(slot), null, Set.of(), Set.of(), Set.of());

        assertThat(routine.getEstimatedDuration()).isEqualTo(Duration.ofSeconds(120));
    }

    @Test
    void reconstitute_setsAllFieldsIncludingId() {
        Routine routine = Routine.reconstitute(5L, 1L, "Squat Day", "desc", Level.ADVANCED,
                null, Duration.ofMinutes(10), List.of(), null, MuscleSummary.empty(),
                Set.of(), Set.of(), Set.of(), null);

        assertThat(routine.getId()).isEqualTo(5L);
        assertThat(routine.getTitle()).isEqualTo("Squat Day");
        assertThat(routine.getLevel()).isEqualTo(Level.ADVANCED);
        assertThat(routine.getEstimatedDuration()).isEqualTo(Duration.ofMinutes(10));
    }

    @Test
    void update_onlyOverwritesNonNullFields() {
        Routine routine = Routine.reconstitute(1L, 1L, "Push Day", "old desc", Level.INTERMEDIATE,
                null, Duration.ZERO, List.of(), null, MuscleSummary.empty(),
                Set.of(), Set.of(), Set.of(), null);

        routine.update(null, "new desc", null, null, false,
                null, null, null, null, null);

        assertThat(routine.getTitle()).isEqualTo("Push Day");
        assertThat(routine.getDescription()).isEqualTo("new desc");
        assertThat(routine.getLevel()).isEqualTo(Level.INTERMEDIATE);
    }

    @Test
    void update_rejectsDuplicatePositionsInNewSlots() {
        Routine routine = Routine.reconstitute(1L, 1L, "Push Day", null, Level.INTERMEDIATE,
                null, Duration.ZERO, List.of(), null, MuscleSummary.empty(),
                Set.of(), Set.of(), Set.of(), null);
        List<ExerciseSlot> duplicated = List.of(
                slotWithOneRepsSet(1L, 1, null),
                slotWithOneRepsSet(2L, 1, null));

        assertThatThrownBy(() -> routine.update(null, null, null, null, false,
                duplicated, null, null, null, null))
                .isInstanceOf(InvalidSlotException.class);
    }

    @Test
    void update_recomputesEstimatedDurationFromNewSlots() {
        Routine routine = Routine.reconstitute(1L, 1L, "Push Day", null, Level.INTERMEDIATE,
                null, Duration.ZERO, List.of(), null, MuscleSummary.empty(),
                Set.of(), Set.of(), Set.of(), null);
        List<ExerciseSlot> newSlots = List.of(slotWithOneRepsSet(1L, 1, null));

        routine.update(null, null, null, null, false,
                newSlots, null, null, null, null);

        assertThat(routine.getSlots()).hasSize(1);
        assertThat(routine.getEstimatedDuration()).isEqualTo(Duration.ofSeconds(45));
    }

    @Test
    void applyMuscleSummary_overwritesSummary() {
        Routine routine = Routine.reconstitute(1L, 1L, "Push Day", null, Level.INTERMEDIATE,
                null, Duration.ZERO, List.of(), null, MuscleSummary.empty(),
                Set.of(), Set.of(), Set.of(), null);
        MuscleSummary summary = MuscleSummary.of(java.util.Map.of(1L, 0.8));

        routine.applyMuscleSummary(summary);

        assertThat(routine.getMuscleSummary().getScore(1L)).isEqualTo(0.8);
    }
}
