package com.alessandropesole.bonwoapp.media.application.dto;

import com.alessandropesole.bonwoapp.media.domain.model.VideoStatus;
import java.time.Instant;

public record VideoResponse(
        Long id,
        String url,
        String thumbnailUrl,
        Integer durationSeconds,
        VideoStatus status,
        Instant createdAt
) {}
