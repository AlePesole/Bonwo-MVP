package com.alessandropesole.bonwoapp.user.application.dto;

import java.util.List;

import com.alessandropesole.bonwoapp.catalog.application.dto.ActivityResponse;
import com.alessandropesole.bonwoapp.media.application.dto.ImageResponse;

public record UserProfileResponse(
        Long userId,
        String username,
        ImageResponse avatar,
        String bio,
        Integer ageYears,
        Integer heightCm,
        Double weightKg,
        List<ActivityResponse> activities
) {}
