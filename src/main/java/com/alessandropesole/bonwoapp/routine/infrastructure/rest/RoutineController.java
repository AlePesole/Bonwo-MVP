package com.alessandropesole.bonwoapp.routine.infrastructure.rest;

import com.alessandropesole.bonwoapp.routine.application.dto.CreateRoutineRequest;
import com.alessandropesole.bonwoapp.routine.application.dto.RoutineResponse;
import com.alessandropesole.bonwoapp.routine.application.dto.UpdateRoutineRequest;
import com.alessandropesole.bonwoapp.routine.domain.model.RoutineFilter;
import com.alessandropesole.bonwoapp.routine.domain.port.in.RoutineUseCase;
import com.alessandropesole.bonwoapp.user.application.service.CurrentUserResolver;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/routines")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class RoutineController {

    private final RoutineUseCase routineUseCase;
    private final CurrentUserResolver currentUserResolver;

    @Operation(
            summary = "List my routines",
            description = "Returns a paginated list of the caller's own routines, optionally filtered " +
                    "by equipment, activity and training goal."
    )
    @GetMapping
    public ResponseEntity<Page<RoutineResponse>> listMine(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(required = false) Long muscleGroupId,
            @RequestParam(required = false) Long muscleSubGroupId,
            @RequestParam(required = false) Set<Long> equipmentIds,
            @RequestParam(required = false) Set<Long> activityIds,
            @RequestParam(required = false) Set<Long> trainingGoalIds,
            @RequestParam(required = false) String title,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        Long userId = currentUserResolver.resolveId(principal);
        var filter = new RoutineFilter(muscleGroupId, muscleSubGroupId, equipmentIds, activityIds, trainingGoalIds, title);
        return ResponseEntity.ok(routineUseCase.listMine(userId, filter, pageable));
    }

    @Operation(
            summary = "Get a routine",
            description = "Returns a single routine by id. Only the owner can access it."
    )
    @GetMapping("/{id}")
    public ResponseEntity<RoutineResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        Long userId = currentUserResolver.resolveId(principal);
        return ResponseEntity.ok(routineUseCase.getById(id, userId));
    }

    @Operation(
            summary = "Create a routine",
            description = "Creates a new personal routine owned by the caller."
    )
    @PostMapping
    public ResponseEntity<RoutineResponse> create(
            @Valid @RequestBody CreateRoutineRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        Long userId = currentUserResolver.resolveId(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(routineUseCase.create(request, userId));
    }

    @Operation(
            summary = "Update a routine",
            description = "Partially updates an existing routine — omitted fields are left unchanged. " +
                    "Only the owner can update it."
    )
    @PutMapping("/{id}")
    public ResponseEntity<RoutineResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoutineRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        Long userId = currentUserResolver.resolveId(principal);
        return ResponseEntity.ok(routineUseCase.update(id, request, userId));
    }

    @Operation(
            summary = "Delete a routine",
            description = "Deletes a routine and its thumbnail. Only the owner can delete it."
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        Long userId = currentUserResolver.resolveId(principal);
        routineUseCase.delete(id, userId);
        return ResponseEntity.noContent().build();
    }
}
