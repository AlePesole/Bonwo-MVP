package com.alessandropesole.bonwoapp.exercise.infrastructure.persistence.mapper.publication;

import com.alessandropesole.bonwoapp.exercise.domain.model.publication.ExercisePublication;
import com.alessandropesole.bonwoapp.exercise.infrastructure.persistence.entity.publication.ExercisePublicationJpaEntity;

public final class ExercisePublicationMapper {

    private ExercisePublicationMapper() {
    }

    public static ExercisePublication toDomain(ExercisePublicationJpaEntity e) {
        return ExercisePublication.reconstitute(
                e.getId(), e.getExerciseId(), e.getAuthorId(), e.getType(), e.getVisibility(),
                e.getLikesCount(), e.getSavesCount(), e.getViewsCount(), e.getUsesCount(),
                e.getPublishedAt()
        );
    }

    public static ExercisePublicationJpaEntity toEntity(ExercisePublication p) {
        return ExercisePublicationJpaEntity.builder()
                .id(p.getId())
                .exerciseId(p.getExerciseId())
                .authorId(p.getAuthorId())
                .type(p.getType())
                .visibility(p.getVisibility())
                .likesCount(p.getLikesCount())
                .savesCount(p.getSavesCount())
                .viewsCount(p.getViewsCount())
                .usesCount(p.getUsesCount())
                .publishedAt(p.getPublishedAt())
                .build();
    }
}
