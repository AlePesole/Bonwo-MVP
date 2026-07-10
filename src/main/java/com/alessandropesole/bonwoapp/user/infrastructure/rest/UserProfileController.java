package com.alessandropesole.bonwoapp.user.infrastructure.rest;

import com.alessandropesole.bonwoapp.user.application.service.CurrentUserResolver;
import com.alessandropesole.bonwoapp.user.application.dto.UpdateProfileRequest;
import com.alessandropesole.bonwoapp.user.application.dto.UserProfileResponse;
import com.alessandropesole.bonwoapp.user.domain.port.in.UserProfileUseCase;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileUseCase profileUseCase;
    private final CurrentUserResolver currentUserResolver;

    @Operation(
            summary = "Get my profile",
            description = "Returns the full profile of the currently authenticated user."
    )
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponse> getMyProfile(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(
                profileUseCase.getMyProfile(currentUserResolver.resolveId(principal)));
    }

    @Operation(
            summary = "Get a public profile",
            description = "Returns the public profile of the user with the given username. " +
                    "No authentication required."
    )
    @GetMapping("/{username}")
    public ResponseEntity<UserProfileResponse> getPublicProfile(
            @PathVariable String username) {
        return ResponseEntity.ok(profileUseCase.getPublicProfile(username));
    }

    @Operation(
            summary = "Update my profile",
            description = "Updates one or more fields of the currently authenticated user's profile. " +
                    "Fields left out of the request body keep their current value."
    )
    @PatchMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        Long userId = currentUserResolver.resolveId(principal);
        return ResponseEntity.ok(profileUseCase.updateProfile(userId, request));
    }
}
