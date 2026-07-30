package com.alessandropesole.bonwoapp.routine.infrastructure.persistence.entity;

import com.alessandropesole.bonwoapp.exercise.domain.model.Level;
import jakarta.persistence.*;
import lombok.*;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "routines", indexes = {
        @Index(name = "idx_routines_owner", columnList = "owner_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RoutineJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Level level;

    @Column(name = "thumbnail_id")
    private Long thumbnailId;

    @Column(name = "estimated_duration", nullable = false)
    private Duration estimatedDuration;

    @Column(name = "rest_between_exercises")
    private Duration restBetweenExercises;

    @OneToMany(mappedBy = "routine", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    @Builder.Default
    private List<ExerciseSlotJpaEntity> slots = new ArrayList<>();

    @Column(name = "muscle_summary_json", columnDefinition = "TEXT")
    private String muscleSummaryJson;

    @ElementCollection
    @CollectionTable(name = "routine_equipment",
            joinColumns = @JoinColumn(name = "routine_id"))
    @Column(name = "equipment_id")
    @Builder.Default
    private List<Long> equipmentIds = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "routine_activities",
            joinColumns = @JoinColumn(name = "routine_id"))
    @Column(name = "activity_id")
    @Builder.Default
    private List<Long> activityIds = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "routine_training_goals",
            joinColumns = @JoinColumn(name = "routine_id"))
    @Column(name = "training_goal_id")
    @Builder.Default
    private List<Long> trainingGoalIds = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "training_program_id")
    private Long trainingProgramId;

    @Column(name = "position")
    private Integer position;
}
