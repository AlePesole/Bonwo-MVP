package com.alessandropesole.bonwoapp.routine.domain.model;

import com.alessandropesole.bonwoapp.routine.domain.exception.InvalidSlotException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExerciseSlotTest {

    private static final List<SetConfig> ONE_SET = List.of(SetConfig.reps(10, null, null));

    @Test
    void create_setsFields() {
        ExerciseSlot slot = ExerciseSlot.create(5L, 1, ONE_SET, Duration.ofSeconds(30));

        assertThat(slot.getExerciseId()).isEqualTo(5L);
        assertThat(slot.getPosition()).isEqualTo(1);
        assertThat(slot.getSets()).hasSize(1);
        assertThat(slot.getRestBetweenSets()).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void create_rejectsNullExerciseId() {
        assertThatThrownBy(() -> ExerciseSlot.create(null, 1, ONE_SET, null))
                .isInstanceOf(InvalidSlotException.class);
    }

    @Test
    void create_rejectsNullOrEmptySets() {
        assertThatThrownBy(() -> ExerciseSlot.create(5L, 1, null, null))
                .isInstanceOf(InvalidSlotException.class);
        assertThatThrownBy(() -> ExerciseSlot.create(5L, 1, List.of(), null))
                .isInstanceOf(InvalidSlotException.class);
    }

    @Test
    void create_rejectsPositionBelowOne() {
        assertThatThrownBy(() -> ExerciseSlot.create(5L, 0, ONE_SET, null))
                .isInstanceOf(InvalidSlotException.class);
    }

    @Test
    void withSets_replacesSetsAndRestKeepingExerciseIdAndPosition() {
        ExerciseSlot slot = ExerciseSlot.create(5L, 2, ONE_SET, Duration.ofSeconds(30));
        List<SetConfig> newSets = List.of(SetConfig.reps(5, null, null), SetConfig.reps(5, null, null));

        ExerciseSlot updated = slot.withSets(newSets, Duration.ofSeconds(60));

        assertThat(updated.getExerciseId()).isEqualTo(5L);
        assertThat(updated.getPosition()).isEqualTo(2);
        assertThat(updated.getSets()).hasSize(2);
        assertThat(updated.getRestBetweenSets()).isEqualTo(Duration.ofSeconds(60));
    }

    @Test
    void withSets_rejectsNullOrEmptySets() {
        ExerciseSlot slot = ExerciseSlot.create(5L, 1, ONE_SET, null);

        assertThatThrownBy(() -> slot.withSets(null, null))
                .isInstanceOf(InvalidSlotException.class);
        assertThatThrownBy(() -> slot.withSets(List.of(), null))
                .isInstanceOf(InvalidSlotException.class);
    }
}
