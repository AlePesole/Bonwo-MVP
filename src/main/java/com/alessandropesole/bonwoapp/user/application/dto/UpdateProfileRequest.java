package com.alessandropesole.bonwoapp.user.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record UpdateProfileRequest(
        String avatarUploadToken,
        @Size(max = 500) String bio,
        @Min(13) @Max(120) Integer ageYears,
        @Min(50) @Max(300) Integer heightCm,
        @Min(20) @Max(500) Double weightKg,
        Set<Long> activityIds
) {}
