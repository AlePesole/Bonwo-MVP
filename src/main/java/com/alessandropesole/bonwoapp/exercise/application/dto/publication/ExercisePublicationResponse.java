package com.alessandropesole.bonwoapp.exercise.application.dto.publication;

import com.alessandropesole.bonwoapp.exercise.application.dto.ExerciseResponse;
import com.alessandropesole.bonwoapp.exercise.domain.model.publication.PublicationType;
import com.alessandropesole.bonwoapp.exercise.domain.model.publication.Visibility;
import com.alessandropesole.bonwoapp.media.application.dto.ImageResponse;

import java.time.Instant;

public record ExercisePublicationResponse(
        Long id,
        ExerciseResponse exercise,
        Long authorId,
        String authorUsername,
        ImageResponse authorAvatar,
        PublicationType type,
        Visibility visibility,
        long likesCount,
        long savesCount,
        long viewsCount,
        long usesCount,
        boolean likedByMe,
        boolean savedByMe,
        Instant publishedAt
) {}
