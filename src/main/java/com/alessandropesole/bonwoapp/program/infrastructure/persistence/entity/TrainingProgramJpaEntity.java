package com.alessandropesole.bonwoapp.program.infrastructure.persistence.entity;

import com.alessandropesole.bonwoapp.exercise.domain.model.Level;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "training_programs", indexes = {
        @Index(name = "idx_training_programs_owner", columnList = "owner_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingProgramJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    @Column(name = "days_per_week", nullable = false)
    private int daysPerWeek;

    @Column(name = "muscle_summary_json", columnDefinition = "TEXT")
    private String muscleSummaryJson;

    @ElementCollection
    @CollectionTable(name = "training_program_equipment",
            joinColumns = @JoinColumn(name = "training_program_id"))
    @Column(name = "equipment_id")
    @Builder.Default
    private List<Long> equipmentIds = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "training_program_activities",
            joinColumns = @JoinColumn(name = "training_program_id"))
    @Column(name = "activity_id")
    @Builder.Default
    private List<Long> activityIds = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "training_program_training_goals",
            joinColumns = @JoinColumn(name = "training_program_id"))
    @Column(name = "training_goal_id")
    @Builder.Default
    private List<Long> trainingGoalIds = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
