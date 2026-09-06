package com.alessandropesole.bonwoapp.routine.domain.model;

import com.alessandropesole.bonwoapp.exercise.domain.model.Level;
import com.alessandropesole.bonwoapp.exercise.domain.model.MuscleSummary;
import com.alessandropesole.bonwoapp.routine.domain.exception.InvalidRoutineTitleException;
import com.alessandropesole.bonwoapp.routine.domain.exception.InvalidSlotException;
import com.alessandropesole.bonwoapp.shared.domain.AggregateRoot;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Getter
public class Routine extends AggregateRoot {

    private Long id;
    private Long ownerId;
    private String title;
    private String description;
    private Level level;
    private Long thumbnailId;
    private Duration estimatedDuration;
    private List<ExerciseSlot> slots;
    private Duration restBetweenExercises;
    private MuscleSummary muscleSummary;
    private Set<Long> equipmentIds;
    private Set<Long> activityIds;
    private Set<Long> trainingGoalIds;
    private Instant createdAt;
    private Long trainingProgramId;
    private Integer position;

    public static Routine create(Long ownerId, String title, String description,
                                 Level level, Long thumbnailId, List<ExerciseSlot> slots,
                                 Duration restBetweenExercises,
                                 Set<Long> equipmentIds, Set<Long> activityIds,
                                 Set<Long> trainingGoalIds) {
        return create(ownerId, title, description, level, thumbnailId, slots, restBetweenExercises,
                equipmentIds, activityIds, trainingGoalIds, null, null);
    }

    public static Routine create(Long ownerId, String title, String description,
                                 Level level, Long thumbnailId, List<ExerciseSlot> slots,
                                 Duration restBetweenExercises,
                                 Set<Long> equipmentIds, Set<Long> activityIds, Set<Long> trainingGoalIds,
                                 Long trainingProgramId, Integer position) {

        if (ownerId == null) throw new IllegalArgumentException("ownerId required");
        validateTitle(title);
        validateNoDuplicatePositions(slots);

        Routine r = new Routine();
        r.ownerId = ownerId;
        r.title = title.strip();
        r.description = description;
        r.level = level != null ? level : Level.INTERMEDIATE;
        r.thumbnailId = thumbnailId;
        r.slots = new ArrayList<>(slots != null ? slots : List.of());
        r.restBetweenExercises = restBetweenExercises;
        r.estimatedDuration = computeDuration(r.slots, restBetweenExercises);
        r.muscleSummary = MuscleSummary.empty();
        r.equipmentIds = new LinkedHashSet<>(equipmentIds != null ? equipmentIds : Set.of());
        r.activityIds = new LinkedHashSet<>(activityIds != null ? activityIds : Set.of());
        r.trainingGoalIds = new LinkedHashSet<>(trainingGoalIds != null ? trainingGoalIds : Set.of());
        r.createdAt = Instant.now();
        r.trainingProgramId = trainingProgramId;
        r.position = position;
        return r;
    }

    public static Routine reconstitute(Long id, Long ownerId, String title, String description,
                                       Level level, Long thumbnailId, Duration estimatedDuration,
                                       List<ExerciseSlot> slots, Duration restBetweenExercises,
                                       MuscleSummary muscleSummary,
                                       Set<Long> equipmentIds, Set<Long> activityIds, Set<Long> trainingGoalIds,
                                       Instant createdAt, Long trainingProgramId, Integer position) {

        Routine r = new Routine();
        r.id = id;
        r.ownerId = ownerId;
        r.title = title;
        r.description = description;
        r.level = level;
        r.thumbnailId = thumbnailId;
        r.estimatedDuration = estimatedDuration;
        r.slots = new ArrayList<>(slots != null ? slots : List.of());
        r.restBetweenExercises = restBetweenExercises;
        r.muscleSummary = muscleSummary != null ? muscleSummary : MuscleSummary.empty();
        r.equipmentIds = new LinkedHashSet<>(equipmentIds != null ? equipmentIds : Set.of());
        r.activityIds = new LinkedHashSet<>(activityIds != null ? activityIds : Set.of());
        r.trainingGoalIds = new LinkedHashSet<>(trainingGoalIds != null ? trainingGoalIds : Set.of());
        r.createdAt = createdAt;
        r.trainingProgramId = trainingProgramId;
        r.position = position;
        return r;
    }

    public void update(String title, String description,
                       Level level, Long thumbnailId, boolean removeThumbnail,
                       List<ExerciseSlot> newSlots,
                       Duration restBetweenExercises,
                       Set<Long> equipmentIds, Set<Long> activityIds,
                       Set<Long> trainingGoalIds) {
        update(title, description, level, thumbnailId, removeThumbnail, newSlots, restBetweenExercises,
                equipmentIds, activityIds, trainingGoalIds, null);
    }

    public void update(String title, String description,
                       Level level, Long thumbnailId, boolean removeThumbnail,
                       List<ExerciseSlot> newSlots,
                       Duration restBetweenExercises,
                       Set<Long> equipmentIds, Set<Long> activityIds,
                       Set<Long> trainingGoalIds, Integer newPosition) {

        if (newPosition != null) this.position = newPosition;
        if (title != null) {
            validateTitle(title);
            this.title = title.strip();
        }
        if (description != null) this.description = description;
        if (level != null) this.level = level;
        if (removeThumbnail) {
            this.thumbnailId = null;
        } else if (thumbnailId != null) {
            this.thumbnailId = thumbnailId;
        }
        if (newSlots != null) {
            validateNoDuplicatePositions(newSlots);
            this.slots = new ArrayList<>(newSlots);
        }
        if (restBetweenExercises != null) this.restBetweenExercises = restBetweenExercises;
        if (equipmentIds != null) this.equipmentIds = new LinkedHashSet<>(equipmentIds);
        if (activityIds != null) this.activityIds = new LinkedHashSet<>(activityIds);
        if (trainingGoalIds != null) this.trainingGoalIds = new LinkedHashSet<>(trainingGoalIds);

        this.estimatedDuration = computeDuration(this.slots, this.restBetweenExercises);

    }

    public void applyMuscleSummary(MuscleSummary summary) {
        this.muscleSummary = summary;
    }

    public boolean isOwnedBy(Long userId) {
        return ownerId != null && ownerId.equals(userId);
    }

    public List<ExerciseSlot> getSlots() {
        return Collections.unmodifiableList(slots);
    }

    private static Duration computeDuration(List<ExerciseSlot> slots, Duration restBetweenExercises) {
        Duration total = Duration.ZERO;
        for (int i = 0; i < slots.size(); i++) {
        ExerciseSlot slot = slots.get(i);
            total = total.plus(setTime(slot)).plus(restBetweenSetsTime(slot));

            boolean hasNextSlot = i < slots.size() - 1;
            if (hasNextSlot && restBetweenExercises != null) {
                total = total.plus(restBetweenExercises);
            }
        }
        return total;
    }

    private static Duration setTime(ExerciseSlot slot) {
        return slot.getSets().stream()
                .map(s -> s.getDuration() != null ? s.getDuration() : Duration.ofSeconds(45))
                .reduce(Duration.ZERO, Duration::plus);
    }

    private static Duration restBetweenSetsTime(ExerciseSlot slot) {
        int restCount = Math.max(0, slot.getSets().size() - 1);
        return slot.getRestBetweenSets() != null
                ? slot.getRestBetweenSets().multipliedBy(restCount)
                : Duration.ZERO;
    }

    private static void validateTitle(String title) {
        if (title == null || title.isBlank() || title.strip().length() > 200) {
            throw new InvalidRoutineTitleException("Routine title must be 1-200 characters");
        }
    }

    private static void validateNoDuplicatePositions(List<ExerciseSlot> slots) {
        if (slots == null || slots.isEmpty()) return;
        long distinctCount = slots.stream().map(ExerciseSlot::getPosition).distinct().count();
        if (distinctCount != slots.size())
            throw new InvalidSlotException("Two slots cannot share the same position");
    }

}
