package com.alessandropesole.bonwoapp.media.application.dto;

import java.time.Instant;

/**
 * Returned after uploading a video.
 * The client must include uploadToken in the entity creation/update request.
 * The token expires at expiresAt (PENDING_TTL_MINUTES minutes from upload).
 */
public record VideoUploadResponse(
        String uploadToken,
        String thumbnailUrl,     // provider-generated preview
        Integer durationSeconds,
        Instant expiresAt
) {}
