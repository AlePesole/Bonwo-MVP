package com.alessandropesole.bonwoapp.exercise.infrastructure.persistence.entity.publication;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "exercise_publication_uses", uniqueConstraints = {
        @UniqueConstraint(name = "uk_publication_use_user", columnNames = {"publication_id", "user_id"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExercisePublicationUseJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "publication_id", nullable = false)
    private Long publicationId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "routine_id", nullable = false)
    private Long routineId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
