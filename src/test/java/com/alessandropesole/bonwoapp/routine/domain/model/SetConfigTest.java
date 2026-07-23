package com.alessandropesole.bonwoapp.routine.domain.model;

import com.alessandropesole.bonwoapp.routine.domain.exception.InvalidSetConfigException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SetConfigTest {

    @Test
    void reps_setsFieldsAndDefaultsWeightModeToTotal() {
        SetConfig set = SetConfig.reps(10, 40.0, null);

        assertThat(set.getType()).isEqualTo(SetType.REPS);
        assertThat(set.getReps()).isEqualTo(10);
        assertThat(set.getWeightKg()).isEqualTo(40.0);
        assertThat(set.getWeightMode()).isEqualTo(WeightMode.TOTAL);
        assertThat(set.getDuration()).isNull();
    }

    @Test
    void reps_weightModeIsNullWhenNoWeight() {
        SetConfig set = SetConfig.reps(10, null, null);

        assertThat(set.getWeightMode()).isNull();
    }

    @Test
    void reps_rejectsZeroOrNegativeReps() {
        assertThatThrownBy(() -> SetConfig.reps(0, null, null))
                .isInstanceOf(InvalidSetConfigException.class);
        assertThatThrownBy(() -> SetConfig.reps(-5, null, null))
                .isInstanceOf(InvalidSetConfigException.class);
    }

    @Test
    void reps_rejectsNegativeWeight() {
        assertThatThrownBy(() -> SetConfig.reps(10, -1.0, WeightMode.TOTAL))
                .isInstanceOf(InvalidSetConfigException.class);
    }

    @Test
    void timed_setsDurationAndType() {
        SetConfig set = SetConfig.timed(Duration.ofSeconds(45), 20.0, WeightMode.PER_SIDE);

        assertThat(set.getType()).isEqualTo(SetType.TIMED);
        assertThat(set.getDuration()).isEqualTo(Duration.ofSeconds(45));
        assertThat(set.getReps()).isZero();
    }

    @Test
    void timed_rejectsNullOrNonPositiveDuration() {
        assertThatThrownBy(() -> SetConfig.timed(null, null, null))
                .isInstanceOf(InvalidSetConfigException.class);
        assertThatThrownBy(() -> SetConfig.timed(Duration.ZERO, null, null))
                .isInstanceOf(InvalidSetConfigException.class);
        assertThatThrownBy(() -> SetConfig.timed(Duration.ofSeconds(-10), null, null))
                .isInstanceOf(InvalidSetConfigException.class);
    }

    @Test
    void amrap_rejectsNonPositiveDuration() {
        assertThatThrownBy(() -> SetConfig.amrap(Duration.ZERO))
                .isInstanceOf(InvalidSetConfigException.class);
    }

    @Test
    void toFailure_setsTypeWithNoRepsOrDuration() {
        SetConfig set = SetConfig.toFailure(30.0, WeightMode.TOTAL);

        assertThat(set.getType()).isEqualTo(SetType.FAILURE);
        assertThat(set.getReps()).isZero();
        assertThat(set.getDuration()).isNull();
        assertThat(set.getWeightKg()).isEqualTo(30.0);
    }
}
