package com.alessandropesole.bonwoapp.catalog.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateMuscleSubGroupRequest(
        @NotNull Long groupId,
        @NotBlank @Size(min = 3, max = 100) String name,
        String detail,
        String svgPathFront,
        String svgPathBack,
        String iconUploadToken
) {}
