package com.alessandropesole.bonwoapp.session.domain.model;

import com.alessandropesole.bonwoapp.session.domain.exception.InvalidTrainingSlotException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TrainingSlotTest {

    @Test
    void isDone_falseWhenAnySetIsNotDone() {
        TrainingSlot slot = TrainingSlot.create(1L, 1,
                List.of(TrainingSet.reps(10, null, null, true), TrainingSet.reps(10, null, null, false)), null);

        assertThat(slot.isDone()).isFalse();
    }

    @Test
    void isDone_trueWhenAllSetsAreDone() {
        TrainingSlot slot = TrainingSlot.create(1L, 1,
                List.of(TrainingSet.reps(10, null, null, true), TrainingSet.reps(10, null, null, true)), null);

        assertThat(slot.isDone()).isTrue();
    }

    @Test
    void create_rejectsEmptySets() {
        assertThatThrownBy(() -> TrainingSlot.create(1L, 1, List.of(), null))
                .isInstanceOf(InvalidTrainingSlotException.class);
    }

    @Test
    void create_rejectsNullExerciseId() {
        assertThatThrownBy(() -> TrainingSlot.create(null, 1,
                List.of(TrainingSet.reps(10, null, null, false)), null))
                .isInstanceOf(InvalidTrainingSlotException.class);
    }
}
