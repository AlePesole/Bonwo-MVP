package com.alessandropesole.bonwoapp.catalog.infrastructure.rest;

import com.alessandropesole.bonwoapp.catalog.application.dto.MuscleGroupRequest;
import com.alessandropesole.bonwoapp.catalog.application.dto.CreateMuscleSubGroupRequest;
import com.alessandropesole.bonwoapp.catalog.application.dto.MuscleGroupResponse;
import com.alessandropesole.bonwoapp.catalog.application.dto.MuscleSubGroupResponse;
import com.alessandropesole.bonwoapp.catalog.application.dto.UpdateMuscleSubGroupRequest;
import com.alessandropesole.bonwoapp.catalog.domain.port.in.MuscleGroupUseCase;
import com.alessandropesole.bonwoapp.catalog.domain.port.in.MuscleSubGroupUseCase;
import com.alessandropesole.bonwoapp.user.application.service.CurrentUserResolver;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/catalog/muscles")
@RequiredArgsConstructor
public class MuscleGroupController {

    private final MuscleGroupUseCase muscleGroupUseCase;
    private final MuscleSubGroupUseCase muscleSubGroupUseCase;
    private final CurrentUserResolver currentUserResolver;

    @Operation(
            summary = "List muscle groups",
            description = "Returns all muscle groups in the catalog, each with its nested sub-groups. No authentication required."
    )
    @GetMapping
    public ResponseEntity<List<MuscleGroupResponse>> listAll() {
        return ResponseEntity.ok(muscleGroupUseCase.listAll());
    }

    @Operation(
            summary = "Create a muscle group",
            description = "Adds a new muscle group to the catalog. Admin only."
    )
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MuscleGroupResponse> createGroup(
            @Valid @RequestBody MuscleGroupRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(muscleGroupUseCase.create(req, currentUserResolver.resolveId(principal)));
    }

    @Operation(
            summary = "Update a muscle group",
            description = "Updates an existing muscle group's details. Admin only."
    )
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MuscleGroupResponse> updateGroup(
            @PathVariable Long id,
            @Valid @RequestBody MuscleGroupRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(
                muscleGroupUseCase.update(id, req, currentUserResolver.resolveId(principal)));
    }

    @Operation(
            summary = "Delete a muscle group",
            description = "Removes a muscle group and cascades to its sub-groups. Admin only."
    )
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteGroup(@PathVariable Long id) {
        muscleGroupUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }


    @Operation(
            summary = "Create a muscle sub-group",
            description = "Adds a new muscle sub-group to an existing muscle group. Admin only."
    )
    @PostMapping("/sub-groups")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MuscleSubGroupResponse> createSubGroup(
            @Valid @RequestBody CreateMuscleSubGroupRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(muscleSubGroupUseCase.create(req, currentUserResolver.resolveId(principal)));
    }

    @Operation(
            summary = "Update a muscle sub-group",
            description = "Updates an existing muscle sub-group's details. Admin only."
    )
    @PutMapping("/sub-groups/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MuscleSubGroupResponse> updateSubGroup(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMuscleSubGroupRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(
                muscleSubGroupUseCase.update(id, req, currentUserResolver.resolveId(principal)));
    }

    @Operation(
            summary = "Delete a muscle sub-group",
            description = "Removes a muscle sub-group from the catalog. Admin only."
    )
    @DeleteMapping("/sub-groups/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteSubGroup(@PathVariable Long id) {
        muscleSubGroupUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }
}
