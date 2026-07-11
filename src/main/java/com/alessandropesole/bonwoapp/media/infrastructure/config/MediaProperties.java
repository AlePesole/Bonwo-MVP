package com.alessandropesole.bonwoapp.media.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.media")
public record MediaProperties(long pendingTtlMinutes) {
}
