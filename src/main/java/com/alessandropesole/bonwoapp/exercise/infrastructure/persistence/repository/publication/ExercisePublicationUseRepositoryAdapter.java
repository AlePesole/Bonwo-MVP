package com.alessandropesole.bonwoapp.exercise.infrastructure.persistence.repository.publication;

import com.alessandropesole.bonwoapp.exercise.domain.port.out.publication.ExercisePublicationUseRepository;
import com.alessandropesole.bonwoapp.exercise.infrastructure.persistence.entity.publication.ExercisePublicationUseJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class ExercisePublicationUseRepositoryAdapter implements ExercisePublicationUseRepository {

    private final ExercisePublicationUseJpaRepository jpa;

    @Override
    public boolean exists(Long publicationId, Long userId) {
        return jpa.existsByPublicationIdAndUserId(publicationId, userId);
    }

    @Override
    public void add(Long publicationId, Long userId, Long routineId) {
        jpa.save(ExercisePublicationUseJpaEntity.builder()
                .publicationId(publicationId)
                .userId(userId)
                .routineId(routineId)
                .createdAt(Instant.now())
                .build());
    }
}
