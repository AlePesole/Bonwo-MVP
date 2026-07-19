package com.alessandropesole.bonwoapp.catalog.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MuscleGroupRequest(
        @NotBlank @Size(min = 3, max = 100) String name,
        String iconUploadToken
) {}
