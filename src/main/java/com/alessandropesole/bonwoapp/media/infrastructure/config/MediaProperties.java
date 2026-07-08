package com.alessandropesole.bonwoapp.media.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Media configuration properties.
 *
 * pendingTtlMinutes → how long a PENDING video/image upload token is valid.
 *                     Defaults to 15 minutes. Change via MEDIA_PENDING_TTL_MINUTES env var.
 *
 * Single source of truth — used by Video, Image and ExpiredMediaCleanupScheduler.
 */
@ConfigurationProperties(prefix = "app.media")
public record MediaProperties(long pendingTtlMinutes) {}
