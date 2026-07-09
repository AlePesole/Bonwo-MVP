package com.alessandropesole.bonwoapp.user.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Pattern(regexp = "^[a-zA-Z0-9_]{1,30}$",
                message = "Username must be 1-30 alphanumeric characters or underscores")
        String username
) {}
