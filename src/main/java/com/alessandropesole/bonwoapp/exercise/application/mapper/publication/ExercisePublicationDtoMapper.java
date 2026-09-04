package com.alessandropesole.bonwoapp.exercise.application.mapper.publication;

import com.alessandropesole.bonwoapp.exercise.application.dto.publication.ExercisePublicationResponse;
import com.alessandropesole.bonwoapp.exercise.application.dto.ExerciseResponse;
import com.alessandropesole.bonwoapp.exercise.domain.model.publication.ExercisePublication;
import com.alessandropesole.bonwoapp.media.application.dto.ImageResponse;

public final class ExercisePublicationDtoMapper {

    private ExercisePublicationDtoMapper() {
    }

    public static ExercisePublicationResponse toResponse(ExercisePublication p, ExerciseResponse exercise,
                                                          String authorUsername, ImageResponse authorAvatar,
                                                          boolean likedByMe, boolean savedByMe) {
        return new ExercisePublicationResponse(
                p.getId(), exercise, p.getAuthorId(), authorUsername, authorAvatar, p.getType(), p.getVisibility(),
                p.getLikesCount(), p.getSavesCount(), p.getViewsCount(), p.getUsesCount(),
                likedByMe, savedByMe, p.getPublishedAt()
        );
    }
}
