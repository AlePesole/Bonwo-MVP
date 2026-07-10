package com.alessandropesole.bonwoapp.user.infrastructure.rest;

import com.alessandropesole.bonwoapp.user.application.dto.*;
import com.alessandropesole.bonwoapp.user.domain.port.in.AuthUseCase;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthUseCase authUseCase;

    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account with the given email, username and password. " +
                    "Fails if the email or username is already taken."
    )
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authUseCase.register(request));
    }

    @Operation(
            summary = "Log in",
            description = "Authenticates a user with email and password, and returns a new access " +
                    "token and refresh token pair."
    )
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(authUseCase.login(request));
    }

    @Operation(
            summary = "Refresh the access token",
            description = "Exchanges a valid, unrevoked refresh token for a new access token and refresh " +
                    "token pair. The refresh token used in this call is revoked immediately after use."
    )
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestParam String refreshToken) {
        return ResponseEntity.ok(authUseCase.refreshToken(refreshToken));
    }

    @Operation(
            summary = "Log out",
            description = "Revokes the given refresh token, so it can no longer be used to obtain new " +
                    "access tokens."
    )
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestParam String refreshToken) {
        authUseCase.logout(refreshToken);
        return ResponseEntity.noContent().build();
    }
}
