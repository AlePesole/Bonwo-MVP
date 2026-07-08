package com.alessandropesole.bonwoapp.media.domain.model;

/**
 * PENDING → uploaded, awaiting claim. Has uploadToken + expiresAt.
 * ACTIVE  → claimed by an entity. uploadToken is null.
 */
public enum ImageStatus {
    PENDING,
    ACTIVE
}
