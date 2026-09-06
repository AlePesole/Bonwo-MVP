package com.alessandropesole.bonwoapp.session.domain.model;

import com.alessandropesole.bonwoapp.exercise.domain.model.MuscleSummary;
import com.alessandropesole.bonwoapp.session.domain.exception.InvalidTrainingSlotException;
import com.alessandropesole.bonwoapp.session.domain.exception.SessionAlreadyCompletedException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TrainingSessionTest {

    private static TrainingSlot slotWithOneRepsSet(long exerciseId, int position, boolean done) {
        return TrainingSlot.create(exerciseId, position,
                List.of(TrainingSet.reps(10, null, null, done)), null);
    }

    @Test
    void start_setsInProgressStatusAndStartedAt() {
        TrainingSession session = TrainingSession.start(1L, 10L, "Push Day", null,
                List.of(slotWithOneRepsSet(5L, 1, false)));

        assertThat(session.getStatus()).isEqualTo(SessionStatus.IN_PROGRESS);
        assertThat(session.getStartedAt()).isNotNull();
        assertThat(session.getCompletedAt()).isNull();
        assertThat(session.getRoutineId()).isEqualTo(10L);
        assertThat(session.getRoutineTitle()).isEqualTo("Push Day");
        assertThat(session.getSlots()).hasSize(1);
    }

    @Test
    void start_rejectsNullRoutineId() {
        assertThatThrownBy(() -> TrainingSession.start(1L, null, "Push Day", null, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("routineId");
    }

    @Test
    void start_rejectsDuplicateSlotPositions() {
        List<TrainingSlot> slots = List.of(
                slotWithOneRepsSet(1L, 1, false),
                slotWithOneRepsSet(2L, 1, false));

        assertThatThrownBy(() -> TrainingSession.start(1L, 10L, "Push Day", null, slots))
                .isInstanceOf(InvalidTrainingSlotException.class);
    }

    @Test
    void update_replacesSlotsAndNoteWhenProvided() {
        TrainingSession session = TrainingSession.start(1L, 10L, "Push Day", null,
                List.of(slotWithOneRepsSet(5L, 1, false)));
        List<TrainingSlot> newSlots = List.of(slotWithOneRepsSet(5L, 1, true));

        session.update(newSlots, "felt strong today");

        assertThat(session.getSlots().get(0).isDone()).isTrue();
        assertThat(session.getFinalNote()).isEqualTo("felt strong today");
    }

    @Test
    void update_isAllowedAfterCompletion() {
        TrainingSession session = TrainingSession.start(1L, 10L, "Push Day", null,
                List.of(slotWithOneRepsSet(5L, 1, false)));
        session.complete(null);

        session.update(List.of(slotWithOneRepsSet(5L, 1, true)), "fixed a typo");

        assertThat(session.getStatus()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(session.getSlots().get(0).isDone()).isTrue();
        assertThat(session.getFinalNote()).isEqualTo("fixed a typo");
    }

    @Test
    void complete_setsCompletedAtDurationAndStatus() {
        TrainingSession session = TrainingSession.start(1L, 10L, "Push Day", null,
                List.of(slotWithOneRepsSet(5L, 1, true)));

        session.complete("great session");

        assertThat(session.getStatus()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(session.getCompletedAt()).isNotNull();
        assertThat(session.getDuration()).isNotNull();
        assertThat(session.getFinalNote()).isEqualTo("great session");
    }

    @Test
    void complete_rejectsCompletingTwice() {
        TrainingSession session = TrainingSession.start(1L, 10L, "Push Day", null,
                List.of(slotWithOneRepsSet(5L, 1, true)));
        session.complete(null);

        assertThatThrownBy(() -> session.complete(null))
                .isInstanceOf(SessionAlreadyCompletedException.class);
    }

    @Test
    void reconstitute_setsAllFields() {
        Instant startedAt = Instant.parse("2026-01-01T10:00:00Z");
        Instant completedAt = Instant.parse("2026-01-01T10:45:00Z");
        TrainingSession session = TrainingSession.reconstitute(1L, 2L, 10L, "Push Day", 20L,
                SessionStatus.COMPLETED, startedAt, completedAt, Duration.ofMinutes(45), "note",
                List.of(slotWithOneRepsSet(5L, 1, true)), MuscleSummary.of(Map.of(1L, 0.5)));

        assertThat(session.getId()).isEqualTo(1L);
        assertThat(session.getTrainingProgramId()).isEqualTo(20L);
        assertThat(session.getDuration()).isEqualTo(Duration.ofMinutes(45));
        assertThat(session.getMuscleSummary().getScore(1L)).isEqualTo(0.5);
    }

    @Test
    void isOwnedBy_matchesOwnerIdOnly() {
        TrainingSession session = TrainingSession.start(1L, 10L, "Push Day", null,
                List.of(slotWithOneRepsSet(5L, 1, false)));

        assertThat(session.isOwnedBy(1L)).isTrue();
        assertThat(session.isOwnedBy(2L)).isFalse();
    }
}
