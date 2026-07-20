package com.alessandropesole.bonwoapp.exercise.infrastructure.persistence.entity;

import com.alessandropesole.bonwoapp.exercise.domain.model.Level;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "exercises", indexes = {
        @Index(name = "idx_exercises_owner", columnList = "owner_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExerciseJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Level level;

    @Column(name = "thumbnail_id")
    private Long thumbnailId;

    @Column(name = "main_video_id")
    private Long mainVideoId;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @ElementCollection
    @CollectionTable(name = "exercise_muscles",
            joinColumns = @JoinColumn(name = "exercise_id"))
    @OrderBy("activation DESC")
    @Builder.Default
    private List<MuscleEntryEmbeddable> muscles = new ArrayList<>();

    @Column(name = "muscle_summary_json", columnDefinition = "TEXT")
    private String muscleSummaryJson;

    @ElementCollection
    @CollectionTable(name = "exercise_equipment",
            joinColumns = @JoinColumn(name = "exercise_id"))
    @Column(name = "equipment_id")
    @Builder.Default
    private List<Long> equipmentIds = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "exercise_activities",
            joinColumns = @JoinColumn(name = "exercise_id"))
    @Column(name = "activity_id")
    @Builder.Default
    private List<Long> activityIds = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "exercise_training_goals",
            joinColumns = @JoinColumn(name = "exercise_id"))
    @Column(name = "training_goal_id")
    @Builder.Default
    private List<Long> trainingGoalIds = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
