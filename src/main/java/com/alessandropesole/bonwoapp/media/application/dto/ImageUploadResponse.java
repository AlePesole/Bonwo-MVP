package com.alessandropesole.bonwoapp.media.application.dto;

import java.time.Instant;

/**
 * Returned after uploading an image.
 * The client must include uploadToken in the entity creation/update request.
 */
public record ImageUploadResponse(
        String uploadToken,
        String url,
        Instant expiresAt
) {}
