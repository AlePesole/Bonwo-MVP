package com.alessandropesole.bonwoapp.exercise.infrastructure.persistence.repository.publication;

import com.alessandropesole.bonwoapp.exercise.domain.port.out.publication.ExercisePublicationLikeRepository;
import com.alessandropesole.bonwoapp.exercise.infrastructure.persistence.entity.publication.ExercisePublicationLikeJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class ExercisePublicationLikeRepositoryAdapter implements ExercisePublicationLikeRepository {

    private final ExercisePublicationLikeJpaRepository jpa;

    @Override
    public boolean exists(Long publicationId, Long userId) {
        return jpa.existsByPublicationIdAndUserId(publicationId, userId);
    }

    @Override
    public void add(Long publicationId, Long userId) {
        jpa.save(ExercisePublicationLikeJpaEntity.builder()
                .publicationId(publicationId)
                .userId(userId)
                .createdAt(Instant.now())
                .build());
    }

    @Override
    public void remove(Long publicationId, Long userId) {
        jpa.deleteByPublicationIdAndUserId(publicationId, userId);
    }
}
