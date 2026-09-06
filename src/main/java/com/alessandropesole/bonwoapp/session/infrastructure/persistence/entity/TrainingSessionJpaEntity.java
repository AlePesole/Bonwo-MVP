package com.alessandropesole.bonwoapp.session.infrastructure.persistence.entity;

import com.alessandropesole.bonwoapp.session.domain.model.SessionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Duration;
import java.time.Instant;

@Entity
@Table(name = "training_sessions", indexes = {
        @Index(name = "idx_training_sessions_owner", columnList = "owner_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrainingSessionJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "routine_id")
    private Long routineId;

    @Column(name = "routine_title", nullable = false, length = 200)
    private String routineTitle;

    @Column(name = "training_program_id")
    private Long trainingProgramId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    private Duration duration;

    @Column(name = "final_note", columnDefinition = "TEXT")
    private String finalNote;

    /** The mutable core of the session (slots + sets, including done flags) as JSON — a single-row
     *  read-modify-write, chosen over relational child tables to avoid the delete-and-reinsert-whole-
     *  collection behavior Hibernate uses for @ElementCollection on every update (see routine_slot_sets). */
    @Column(name = "slots_json", nullable = false, columnDefinition = "TEXT")
    private String slotsJson;

    @Column(name = "muscle_summary_json", columnDefinition = "TEXT")
    private String muscleSummaryJson;
}
