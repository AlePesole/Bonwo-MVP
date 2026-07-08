package com.alessandropesole.bonwoapp.media.domain.model;

/**
 * Lifecycle state of an uploaded video.
 *<p>
 * PENDING    → uploaded to media storage, waiting to be claimed by an entity.
 *              Has an uploadToken and expiresAt. Cleaned up by nightly job if unclaimed.
 *<p>
 * ACTIVE     → claimed by an entity
 *<p>
 * FAILED     → upload or transcoding failed. Should not be assigned to any entity.
 */
public enum VideoStatus {
    PENDING,
    ACTIVE,
    FAILED
}
