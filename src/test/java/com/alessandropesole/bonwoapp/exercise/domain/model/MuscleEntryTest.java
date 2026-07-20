package com.alessandropesole.bonwoapp.exercise.domain.model;

import com.alessandropesole.bonwoapp.exercise.domain.exception.InvalidMuscleEntryException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MuscleEntryTest {

    @Test
    void of_setsSubGroupIdAndActivation() {
        MuscleEntry entry = MuscleEntry.of(4L, 0.7);

        assertThat(entry.getSubGroupId()).isEqualTo(4L);
        assertThat(entry.getActivation()).isEqualTo(0.7);
    }

    @Test
    void of_roundsActivationToOneDecimal() {
        MuscleEntry entry = MuscleEntry.of(4L, 0.746);

        assertThat(entry.getActivation()).isEqualTo(0.7);
    }

    @Test
    void of_rejectsNullSubGroupId() {
        assertThatThrownBy(() -> MuscleEntry.of(null, 0.5))
                .isInstanceOf(InvalidMuscleEntryException.class);
    }

    @Test
    void of_rejectsActivationBelowMinimum() {
        assertThatThrownBy(() -> MuscleEntry.of(4L, 0.05))
                .isInstanceOf(InvalidMuscleEntryException.class);
    }

    @Test
    void of_rejectsActivationAboveMaximum() {
        assertThatThrownBy(() -> MuscleEntry.of(4L, 1.1))
                .isInstanceOf(InvalidMuscleEntryException.class);
    }

    @Test
    void role_classifiesActivationIntoPrimarySecondaryStabilizer() {
        assertThat(MuscleEntry.of(4L, 0.9).role()).isEqualTo(ActivationLevel.PRIMARY);
        assertThat(MuscleEntry.of(4L, 0.5).role()).isEqualTo(ActivationLevel.SECONDARY);
        assertThat(MuscleEntry.of(4L, 0.2).role()).isEqualTo(ActivationLevel.STABILIZER);
    }
}
