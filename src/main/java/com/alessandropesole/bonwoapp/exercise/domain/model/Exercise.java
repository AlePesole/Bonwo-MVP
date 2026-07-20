package com.alessandropesole.bonwoapp.exercise.domain.model;

import com.alessandropesole.bonwoapp.exercise.domain.exception.InvalidExerciseTitleException;
import com.alessandropesole.bonwoapp.exercise.domain.exception.InvalidMuscleEntryException;
import com.alessandropesole.bonwoapp.shared.domain.AggregateRoot;
import lombok.Getter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Getter
public class Exercise extends AggregateRoot {

    private Long id;
    private Long ownerId;
    private String title;
    private Level level;
    private Long thumbnailId;
    private Long mainVideoId;
    private String description;
    private String instructions;
    private List<MuscleEntry> muscles;
    private MuscleSummary muscleSummary;
    private Set<Long> equipmentIds;
    private Set<Long> activityIds;
    private Set<Long> trainingGoalIds;
    private Instant createdAt;

    public static Exercise create(Long ownerId, String title, Level level,
                                  Long thumbnailId, Long mainVideoId,
                                  String description, String instructions,
                                  List<MuscleEntry> muscles, MuscleSummary muscleSummary,
                                  Set<Long> equipmentIds, Set<Long> activityIds, Set<Long> trainingGoalIds) {

        if (ownerId == null) throw new IllegalArgumentException("ownerId required");
        validateTitle(title);
        validateNoDuplicateSubGroups(muscles);

        Exercise e = new Exercise();
        e.ownerId = ownerId;
        e.title = title.strip();
        e.level = level != null ? level : Level.INTERMEDIATE;
        e.thumbnailId = thumbnailId;
        e.mainVideoId = mainVideoId;
        e.description = description;
        e.instructions = instructions;
        e.muscles = new ArrayList<>(muscles != null ? muscles : List.of());
        e.muscleSummary = muscleSummary != null ? muscleSummary : MuscleSummary.empty();
        e.equipmentIds = new LinkedHashSet<>(equipmentIds != null ? equipmentIds : Set.of());
        e.activityIds = new LinkedHashSet<>(activityIds != null ? activityIds : Set.of());
        e.trainingGoalIds = new LinkedHashSet<>(trainingGoalIds != null ? trainingGoalIds : Set.of());
        e.createdAt = Instant.now();
        return e;
    }

    public static Exercise reconstitute(Long id, Long ownerId, String title, Level level,
                                        Long thumbnailId, Long mainVideoId,
                                        String description, String instructions,
                                        List<MuscleEntry> muscles, MuscleSummary muscleSummary,
                                        Set<Long> equipmentIds, Set<Long> activityIds, Set<Long> trainingGoalIds,
                                        Instant createdAt) {

        Exercise e = new Exercise();
        e.id = id;
        e.ownerId = ownerId;
        e.title = title;
        e.level = level;
        e.thumbnailId = thumbnailId;
        e.mainVideoId = mainVideoId;
        e.description = description;
        e.instructions = instructions;
        e.muscles = new ArrayList<>(muscles != null ? muscles : List.of());
        e.muscleSummary = muscleSummary != null ? muscleSummary : MuscleSummary.empty();
        e.equipmentIds = new LinkedHashSet<>(equipmentIds != null ? equipmentIds : Set.of());
        e.activityIds = new LinkedHashSet<>(activityIds != null ? activityIds : Set.of());
        e.trainingGoalIds = new LinkedHashSet<>(trainingGoalIds != null ? trainingGoalIds : Set.of());
        e.createdAt = createdAt;
        return e;
    }


    public void update(String title, Level level, Long thumbnailId, boolean removeThumbnail,
                       Long mainVideoId, boolean removeMainVideo,
                       String description, String instructions,
                       List<MuscleEntry> muscles, MuscleSummary muscleSummary,
                       Set<Long> equipmentIds, Set<Long> activityIds, Set<Long> trainingGoalIds) {

        if (title != null) {
            validateTitle(title);
            this.title = title.strip();
        }
        if (level != null) this.level = level;
        if (removeThumbnail) {
            this.thumbnailId = null;
        } else if (thumbnailId != null) {
            this.thumbnailId = thumbnailId;
        }
        if (removeMainVideo) {
            this.mainVideoId = null;
        } else if (mainVideoId != null) {
            this.mainVideoId = mainVideoId;
        }
        if (description != null) this.description = description;
        if (instructions != null) this.instructions = instructions;
        if (muscles != null) {
            validateNoDuplicateSubGroups(muscles);
            this.muscles = new ArrayList<>(muscles);
            this.muscleSummary = muscleSummary != null ? muscleSummary : MuscleSummary.empty();
        }
        if (equipmentIds != null) this.equipmentIds = new LinkedHashSet<>(equipmentIds);
        if (activityIds != null) this.activityIds = new LinkedHashSet<>(activityIds);
        if (trainingGoalIds != null) this.trainingGoalIds = new LinkedHashSet<>(trainingGoalIds);
    }

    public boolean isOwnedBy(Long userId) {
        return ownerId != null && ownerId.equals(userId);
    }

    public List<MuscleEntry> getMuscles() {
        return Collections.unmodifiableList(muscles);
    }

    public Set<Long> getEquipmentIds() {
        return Collections.unmodifiableSet(equipmentIds);
    }

    public Set<Long> getActivityIds() {
        return Collections.unmodifiableSet(activityIds);
    }

    public Set<Long> getTrainingGoalIds() {
        return Collections.unmodifiableSet(trainingGoalIds);
    }

    private static void validateTitle(String title) {
        if (title == null || title.isBlank() || title.strip().length() > 200)
            throw new InvalidExerciseTitleException("Exercise title must be 1-200 characters");
    }

    private static void validateNoDuplicateSubGroups(List<MuscleEntry> muscles) {
        if (muscles == null || muscles.isEmpty()) return;
        long distinctCount = muscles.stream().map(MuscleEntry::getSubGroupId).distinct().count();
        if (distinctCount != muscles.size())
            throw new InvalidMuscleEntryException("The same muscle sub-group cannot be listed twice");
    }
}
