package com.alessandropesole.bonwoapp.user.infrastructure.rest;

import com.alessandropesole.bonwoapp.user.application.dto.AdminUpdateUserRequest;
import com.alessandropesole.bonwoapp.user.application.dto.ChangeRoleRequest;
import com.alessandropesole.bonwoapp.user.application.dto.UserResponse;
import com.alessandropesole.bonwoapp.user.domain.port.in.UserManagementUseCase;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserManagementUseCase userManagementUseCase;

    @Operation(
            summary = "List users",
            description = "Returns a paginated list of all users, sorted by creation date by default. " +
                    "Admin only."
    )
    @GetMapping
    public ResponseEntity<Page<UserResponse>> listUsers(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(userManagementUseCase.listUsers(pageable));
    }

    @Operation(
            summary = "Get a user by ID",
            description = "Returns the full details of a single user. Admin only."
    )
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long userId) {
        return ResponseEntity.ok(userManagementUseCase.getUserById(userId));
    }

    @Operation(
            summary = "Update a user",
            description = "Updates a user's account details on behalf of an admin. Admin only."
    )
    @PatchMapping("/{userId}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUpdateUserRequest request) {
        return ResponseEntity.ok(userManagementUseCase.adminUpdateUser(userId, request));
    }

    @Operation(
            summary = "Ban a user",
            description = "Bans the user, preventing them from logging in or using an existing session. " +
                    "Fails if the user is already banned. Admin only."
    )
    @PostMapping("/{userId}/ban")
    public ResponseEntity<Void> banUser(@PathVariable Long userId) {
        userManagementUseCase.banUser(userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Unban a user",
            description = "Lifts a ban on the user, restoring normal access. Fails if the user is not " +
                    "currently banned. Admin only."
    )
    @PostMapping("/{userId}/unban")
    public ResponseEntity<Void> unbanUser(@PathVariable Long userId) {
        userManagementUseCase.unbanUser(userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Delete a user",
            description = "Soft-deletes the user, anonymizing their account data. Admin only."
    )
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        userManagementUseCase.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Change a user's role",
            description = "Promotes or demotes the user between USER and ADMIN roles. Admin only."
    )
    @PatchMapping("/{userId}/role")
    public ResponseEntity<Void> changeRole(
            @PathVariable Long userId,
            @Valid @RequestBody ChangeRoleRequest request) {
        userManagementUseCase.changeRole(userId, request.role());
        return ResponseEntity.noContent().build();
    }
}
