package com.alessandropesole.bonwoapp.user.application.dto;

import com.alessandropesole.bonwoapp.user.domain.model.AccountStatus;
import com.alessandropesole.bonwoapp.user.domain.model.UserRole;
import java.time.Instant;

public record UserResponse(
        Long id,
        String email,
        String username,
        UserRole role,
        AccountStatus status,
        Instant createdAt
) {}
