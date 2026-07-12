package com.alessandropesole.bonwoapp.user.application.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminUpdateUserRequest(
        @Pattern(regexp = "^[a-zA-Z0-9_]{3,30}$") String username,
        @Size(max = 500) String bio
) {}
