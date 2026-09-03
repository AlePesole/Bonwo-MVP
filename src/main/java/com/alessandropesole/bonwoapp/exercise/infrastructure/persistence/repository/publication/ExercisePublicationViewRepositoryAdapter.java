package com.alessandropesole.bonwoapp.exercise.infrastructure.persistence.repository.publication;

import com.alessandropesole.bonwoapp.exercise.domain.port.out.publication.ExercisePublicationViewRepository;
import com.alessandropesole.bonwoapp.exercise.infrastructure.persistence.entity.publication.ExercisePublicationViewJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class ExercisePublicationViewRepositoryAdapter implements ExercisePublicationViewRepository {

    private final ExercisePublicationViewJpaRepository jpa;

    @Override
    public boolean exists(Long publicationId, Long userId) {
        return jpa.existsByPublicationIdAndUserId(publicationId, userId);
    }

    @Override
    public void add(Long publicationId, Long userId) {
        jpa.save(ExercisePublicationViewJpaEntity.builder()
                .publicationId(publicationId)
                .userId(userId)
                .createdAt(Instant.now())
                .build());
    }
}
