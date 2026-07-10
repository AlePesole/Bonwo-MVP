package com.alessandropesole.bonwoapp.user.application.dto;

import com.alessandropesole.bonwoapp.user.domain.model.UserRole;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(
        @NotNull UserRole role
) {}
