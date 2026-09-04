package com.alessandropesole.bonwoapp.exercise.infrastructure.persistence.entity.publication;

import com.alessandropesole.bonwoapp.exercise.domain.model.publication.PublicationType;
import com.alessandropesole.bonwoapp.exercise.domain.model.publication.Visibility;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "exercise_publications", indexes = {
        @Index(name = "idx_exercise_publications_exercise", columnList = "exercise_id"),
        @Index(name = "idx_exercise_publications_author", columnList = "author_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExercisePublicationJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exercise_id", nullable = false)
    private Long exerciseId;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PublicationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Visibility visibility;

    @Column(name = "likes_count", nullable = false)
    private long likesCount;

    @Column(name = "saves_count", nullable = false)
    private long savesCount;

    @Column(name = "views_count", nullable = false)
    private long viewsCount;

    @Column(name = "uses_count", nullable = false)
    private long usesCount;

    @Column(name = "published_at", nullable = false, updatable = false)
    private Instant publishedAt;
}
