package com.alessandropesole.bonwoapp.session.domain.model;

import com.alessandropesole.bonwoapp.exercise.domain.model.MuscleSummary;
import com.alessandropesole.bonwoapp.session.domain.exception.InvalidTrainingSlotException;
import com.alessandropesole.bonwoapp.session.domain.exception.SessionAlreadyCompletedException;
import com.alessandropesole.bonwoapp.shared.domain.AggregateRoot;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A performed (or in-progress) instance of a Routine. Started from a Routine snapshot — title and,
 * if applicable, trainingProgramId are copied at start time so this log survives even if the source
 * Routine is later edited or deleted (routineId is a soft reference, nulled on delete).
 * Deliberately does not store description, level, thumbnail, or the equipment/activity/trainingGoal
 * lists — those belong to the Routine plan, not to a specific performance of it.
 */
@Getter
public class TrainingSession extends AggregateRoot {

    private Long id;
    private Long ownerId;
    private Long routineId;
    private String routineTitle;
    private Long trainingProgramId;
    private SessionStatus status;
    private Instant startedAt;
    private Instant completedAt;
    private Duration duration;
    private String finalNote;
    private List<TrainingSlot> slots;
    private MuscleSummary muscleSummary;

    public static TrainingSession start(Long ownerId, Long routineId, String routineTitle,
                                        Long trainingProgramId, List<TrainingSlot> slots) {
        if (ownerId == null) throw new IllegalArgumentException("ownerId required");
        if (routineId == null) throw new IllegalArgumentException("routineId required");
        validateNoDuplicatePositions(slots);

        TrainingSession s = new TrainingSession();
        s.ownerId = ownerId;
        s.routineId = routineId;
        s.routineTitle = routineTitle;
        s.trainingProgramId = trainingProgramId;
        s.status = SessionStatus.IN_PROGRESS;
        s.startedAt = Instant.now();
        s.slots = new ArrayList<>(slots != null ? slots : List.of());
        s.muscleSummary = MuscleSummary.empty();
        return s;
    }

    public static TrainingSession reconstitute(Long id, Long ownerId, Long routineId, String routineTitle,
                                               Long trainingProgramId, SessionStatus status,
                                               Instant startedAt, Instant completedAt, Duration duration,
                                               String finalNote, List<TrainingSlot> slots,
                                               MuscleSummary muscleSummary) {
        TrainingSession s = new TrainingSession();
        s.id = id;
        s.ownerId = ownerId;
        s.routineId = routineId;
        s.routineTitle = routineTitle;
        s.trainingProgramId = trainingProgramId;
        s.status = status;
        s.startedAt = startedAt;
        s.completedAt = completedAt;
        s.duration = duration;
        s.finalNote = finalNote;
        s.slots = new ArrayList<>(slots != null ? slots : List.of());
        s.muscleSummary = muscleSummary != null ? muscleSummary : MuscleSummary.empty();
        return s;
    }

    /** Adding/removing exercises, editing sets and toggling done all go through here — allowed
     *  in any status, so past sessions can still be corrected once completed. */
    public void update(List<TrainingSlot> newSlots, String finalNote) {
        if (newSlots != null) {
            validateNoDuplicatePositions(newSlots);
            this.slots = new ArrayList<>(newSlots);
        }
        if (finalNote != null) this.finalNote = finalNote;
    }

    /** Terminal transition — once COMPLETED, a session can be edited but never restarted or reopened. */
    public void complete(String finalNote) {
        if (status == SessionStatus.COMPLETED) {
            throw new SessionAlreadyCompletedException("This training session is already completed");
        }
        this.completedAt = Instant.now();
        this.duration = Duration.between(startedAt, completedAt);
        this.status = SessionStatus.COMPLETED;
        if (finalNote != null) this.finalNote = finalNote;
    }

    public void applyMuscleSummary(MuscleSummary summary) {
        this.muscleSummary = summary;
    }

    public boolean isOwnedBy(Long userId) {
        return ownerId != null && ownerId.equals(userId);
    }

    public List<TrainingSlot> getSlots() {
        return Collections.unmodifiableList(slots);
    }

    private static void validateNoDuplicatePositions(List<TrainingSlot> slots) {
        if (slots == null || slots.isEmpty()) return;
        long distinctCount = slots.stream().map(TrainingSlot::getPosition).distinct().count();
        if (distinctCount != slots.size())
            throw new InvalidTrainingSlotException("Two slots cannot share the same position");
    }
}
