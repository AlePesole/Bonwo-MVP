package com.alessandropesole.bonwoapp.program.domain.model;

import com.alessandropesole.bonwoapp.exercise.domain.model.Level;
import com.alessandropesole.bonwoapp.exercise.domain.model.MuscleSummary;
import com.alessandropesole.bonwoapp.program.domain.exception.InvalidTrainingProgramException;
import com.alessandropesole.bonwoapp.shared.domain.AggregateRoot;
import lombok.Getter;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A weekly training program. Its routines are not held here — they are real, independently-addressable
 * Routine rows (see Routine.trainingProgramId/position), owned by this aggregate via that foreign key.
 * TrainingProgramService fetches them via RoutineRepository.findByTrainingProgramId(id) rather than
 * this class caching a list of them.
 */
@Getter
public class TrainingProgram extends AggregateRoot {

    private Long id;
    private Long ownerId;
    private String title;
    private String description;
    private Level level;
    private Long thumbnailId;
    private int daysPerWeek;
    private MuscleSummary muscleSummary;
    private Set<Long> equipmentIds;
    private Set<Long> activityIds;
    private Set<Long> trainingGoalIds;
    private Instant createdAt;

    public static TrainingProgram create(Long ownerId, String title, String description,
                                         Level level, Long thumbnailId, int daysPerWeek,
                                         Set<Long> equipmentIds, Set<Long> activityIds,
                                         Set<Long> trainingGoalIds) {

        if (ownerId == null) throw new IllegalArgumentException("ownerId required");
        validateTitle(title);
        validateDaysPerWeek(daysPerWeek);

        TrainingProgram p = new TrainingProgram();
        p.ownerId = ownerId;
        p.title = title.strip();
        p.description = description;
        p.level = level != null ? level : Level.INTERMEDIATE;
        p.thumbnailId = thumbnailId;
        p.daysPerWeek = daysPerWeek;
        p.muscleSummary = MuscleSummary.empty();
        p.equipmentIds = new LinkedHashSet<>(equipmentIds != null ? equipmentIds : Set.of());
        p.activityIds = new LinkedHashSet<>(activityIds != null ? activityIds : Set.of());
        p.trainingGoalIds = new LinkedHashSet<>(trainingGoalIds != null ? trainingGoalIds : Set.of());
        p.createdAt = Instant.now();
        return p;
    }

    public static TrainingProgram reconstitute(Long id, Long ownerId, String title,
                                               String description, Level level, Long thumbnailId,
                                               int daysPerWeek, MuscleSummary muscleSummary,
                                               Set<Long> equipmentIds, Set<Long> activityIds,
                                               Set<Long> trainingGoalIds, Instant createdAt) {

        TrainingProgram p = new TrainingProgram();
        p.id = id;
        p.ownerId = ownerId;
        p.title = title;
        p.description = description;
        p.level = level;
        p.thumbnailId = thumbnailId;
        p.daysPerWeek = daysPerWeek;
        p.muscleSummary = muscleSummary != null ? muscleSummary : MuscleSummary.empty();
        p.equipmentIds = new LinkedHashSet<>(equipmentIds != null ? equipmentIds : Set.of());
        p.activityIds = new LinkedHashSet<>(activityIds != null ? activityIds : Set.of());
        p.trainingGoalIds = new LinkedHashSet<>(trainingGoalIds != null ? trainingGoalIds : Set.of());
        p.createdAt = createdAt;
        return p;
    }

    public void update(String title, String description, Level level,
                       Long thumbnailId, boolean removeThumbnail,
                       Integer daysPerWeek,
                       Set<Long> equipmentIds, Set<Long> activityIds, Set<Long> trainingGoalIds) {

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
        if (daysPerWeek != null) {
            validateDaysPerWeek(daysPerWeek);
            this.daysPerWeek = daysPerWeek;
        }
        if (equipmentIds != null) this.equipmentIds = new LinkedHashSet<>(equipmentIds);
        if (activityIds != null) this.activityIds = new LinkedHashSet<>(activityIds);
        if (trainingGoalIds != null) this.trainingGoalIds = new LinkedHashSet<>(trainingGoalIds);
    }

    public void applyMuscleSummary(MuscleSummary summary) {
        this.muscleSummary = summary;
    }

    public boolean isOwnedBy(Long userId) {
        return ownerId != null && ownerId.equals(userId);
    }

    private static void validateTitle(String title) {
        if (title == null || title.isBlank() || title.strip().length() > 200) {
            throw new InvalidTrainingProgramException("title must be 1-200 characters");
        }
    }

    private static void validateDaysPerWeek(int days) {
        if (days < 1 || days > 7) {
            throw new InvalidTrainingProgramException("daysPerWeek must be between 1 and 7");
        }
    }
}
