package com.alessandropesole.bonwoapp.exercise.infrastructure.rest;

import com.alessandropesole.bonwoapp.exercise.application.dto.CreateExerciseRequest;
import com.alessandropesole.bonwoapp.exercise.application.dto.ExerciseResponse;
import com.alessandropesole.bonwoapp.exercise.application.dto.UpdateExerciseRequest;
import com.alessandropesole.bonwoapp.exercise.domain.model.ExerciseFilter;
import com.alessandropesole.bonwoapp.exercise.domain.port.in.ExerciseUseCase;
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
@RequestMapping("/exercises")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class ExerciseController {

    private final ExerciseUseCase exerciseUseCase;
    private final CurrentUserResolver currentUserResolver;

    @Operation(
            summary = "List my exercises",
            description = "Returns a paginated list of the caller's own exercises, optionally filtered " +
                    "by muscle group/sub-group, equipment, activity and training goal."
    )
    @GetMapping
    public ResponseEntity<Page<ExerciseResponse>> listMine(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(required = false) Long muscleGroupId,
            @RequestParam(required = false) Long muscleSubGroupId,
            @RequestParam(required = false) Set<Long> equipmentIds,
            @RequestParam(required = false) Set<Long> activityIds,
            @RequestParam(required = false) Set<Long> trainingGoalIds,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        Long userId = currentUserResolver.resolveId(principal);
        var filter = new ExerciseFilter(muscleGroupId, muscleSubGroupId, equipmentIds, activityIds, trainingGoalIds);
        return ResponseEntity.ok(exerciseUseCase.listMine(userId, filter, pageable));
    }

    @Operation(
            summary = "Get an exercise",
            description = "Returns a single exercise by id. Only the owner can access it."
    )
    @GetMapping("/{id}")
    public ResponseEntity<ExerciseResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        Long userId = currentUserResolver.resolveId(principal);
        return ResponseEntity.ok(exerciseUseCase.getById(id, userId));
    }

    @Operation(
            summary = "Create an exercise",
            description = "Creates a new personal exercise owned by the caller."
    )
    @PostMapping
    public ResponseEntity<ExerciseResponse> create(
            @Valid @RequestBody CreateExerciseRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        Long userId = currentUserResolver.resolveId(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(exerciseUseCase.create(request, userId));
    }

    @Operation(
            summary = "Update an exercise",
            description = "Partially updates an existing exercise — omitted fields are left unchanged. " +
                    "Only the owner can update it."
    )
    @PutMapping("/{id}")
    public ResponseEntity<ExerciseResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateExerciseRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        Long userId = currentUserResolver.resolveId(principal);
        return ResponseEntity.ok(exerciseUseCase.update(id, request, userId));
    }

    @Operation(
            summary = "Delete an exercise",
            description = "Deletes an exercise and its associated media (thumbnail/video). " +
                    "Only the owner can delete it."
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        Long userId = currentUserResolver.resolveId(principal);
        exerciseUseCase.delete(id, userId);
        return ResponseEntity.noContent().build();
    }
}
