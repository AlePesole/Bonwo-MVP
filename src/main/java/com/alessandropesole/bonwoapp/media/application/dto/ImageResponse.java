package com.alessandropesole.bonwoapp.media.application.dto;

import java.time.Instant;

public record ImageResponse(
        Long id,
        String url,
        Instant createdAt
) {}
