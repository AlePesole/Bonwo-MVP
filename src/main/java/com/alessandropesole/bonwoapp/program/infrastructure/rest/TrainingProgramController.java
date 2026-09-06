package com.alessandropesole.bonwoapp.program.infrastructure.rest;

import com.alessandropesole.bonwoapp.program.application.dto.CreateTrainingProgramRequest;
import com.alessandropesole.bonwoapp.program.application.dto.TrainingProgramResponse;
import com.alessandropesole.bonwoapp.program.application.dto.UpdateTrainingProgramRequest;
import com.alessandropesole.bonwoapp.program.domain.model.TrainingProgramFilter;
import com.alessandropesole.bonwoapp.program.domain.port.in.TrainingProgramUseCase;
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
@RequestMapping("/training-programs")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class TrainingProgramController {

    private final TrainingProgramUseCase trainingProgramUseCase;
    private final CurrentUserResolver currentUserResolver;

    @Operation(
            summary = "List my training programs",
            description = "Returns a paginated list of the caller's own training programs, optionally " +
                    "filtered by equipment, activity and training goal."
    )
    @GetMapping
    public ResponseEntity<Page<TrainingProgramResponse>> listMine(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(required = false) Set<Long> equipmentIds,
            @RequestParam(required = false) Set<Long> activityIds,
            @RequestParam(required = false) Set<Long> trainingGoalIds,
            @RequestParam(required = false) String title,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        Long userId = currentUserResolver.resolveId(principal);
        var filter = new TrainingProgramFilter(equipmentIds, activityIds, trainingGoalIds, title);
        return ResponseEntity.ok(trainingProgramUseCase.listMine(userId, filter, pageable));
    }

    @Operation(
            summary = "Get a training program",
            description = "Returns a single training program by id. Only the owner can access it."
    )
    @GetMapping("/{id}")
    public ResponseEntity<TrainingProgramResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        Long userId = currentUserResolver.resolveId(principal);
        return ResponseEntity.ok(trainingProgramUseCase.getById(id, userId));
    }

    @Operation(
            summary = "Create a training program",
            description = "Creates a new personal training program owned by the caller."
    )
    @PostMapping
    public ResponseEntity<TrainingProgramResponse> create(
            @Valid @RequestBody CreateTrainingProgramRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        Long userId = currentUserResolver.resolveId(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(trainingProgramUseCase.create(request, userId));
    }

    @Operation(
            summary = "Update a training program",
            description = "Partially updates an existing training program — omitted fields are left " +
                    "unchanged. Only the owner can update it."
    )
    @PutMapping("/{id}")
    public ResponseEntity<TrainingProgramResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTrainingProgramRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        Long userId = currentUserResolver.resolveId(principal);
        return ResponseEntity.ok(trainingProgramUseCase.update(id, request, userId));
    }

    @Operation(
            summary = "Delete a training program",
            description = "Deletes a training program and its thumbnail. Only the owner can delete it."
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        Long userId = currentUserResolver.resolveId(principal);
        trainingProgramUseCase.delete(id, userId);
        return ResponseEntity.noContent().build();
    }
}
