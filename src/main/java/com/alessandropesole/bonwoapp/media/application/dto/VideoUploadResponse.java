package com.alessandropesole.bonwoapp.media.application.dto;

import java.time.Instant;

public record VideoUploadResponse(
        String uploadToken,
        String thumbnailUrl,
        Integer durationSeconds,
        Instant expiresAt
) {}
