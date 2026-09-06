package com.alessandropesole.bonwoapp.media.application.dto;

import java.time.Instant;

public record ImageUploadResponse(
        String uploadToken,
        String url,
        Instant expiresAt
) {}
