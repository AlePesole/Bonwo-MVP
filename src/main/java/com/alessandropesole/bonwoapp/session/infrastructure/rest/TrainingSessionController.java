package com.alessandropesole.bonwoapp.session.infrastructure.rest;

import com.alessandropesole.bonwoapp.session.application.dto.CompleteTrainingSessionRequest;
import com.alessandropesole.bonwoapp.session.application.dto.StartTrainingSessionRequest;
import com.alessandropesole.bonwoapp.session.application.dto.TrainingSessionResponse;
import com.alessandropesole.bonwoapp.session.application.dto.UpdateTrainingSessionRequest;
import com.alessandropesole.bonwoapp.session.domain.port.in.TrainingSessionUseCase;
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

@RestController
@RequestMapping("/training-sessions")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class TrainingSessionController {

    private final TrainingSessionUseCase trainingSessionUseCase;
    private final CurrentUserResolver currentUserResolver;

    @Operation(
            summary = "List my training sessions",
            description = "Returns a paginated history of the caller's own training sessions, most recent first."
    )
    @GetMapping
    public ResponseEntity<Page<TrainingSessionResponse>> listMine(
            @AuthenticationPrincipal UserDetails principal,
            @PageableDefault(size = 20, sort = "startedAt") Pageable pageable) {
        Long userId = currentUserResolver.resolveId(principal);
        return ResponseEntity.ok(trainingSessionUseCase.listMine(userId, pageable));
    }

    @Operation(
            summary = "Get a training session",
            description = "Returns a single training session by id. Only the owner can access it."
    )
    @GetMapping("/{id}")
    public ResponseEntity<TrainingSessionResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        Long userId = currentUserResolver.resolveId(principal);
        return ResponseEntity.ok(trainingSessionUseCase.getById(id, userId));
    }

    @Operation(
            summary = "Start a training session",
            description = "Starts a new in-progress training session by cloning the current exercises and " +
                    "sets of the given routine. Only the routine's owner can start it."
    )
    @PostMapping
    public ResponseEntity<TrainingSessionResponse> start(
            @Valid @RequestBody StartTrainingSessionRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        Long userId = currentUserResolver.resolveId(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(trainingSessionUseCase.start(request, userId));
    }

    @Operation(
            summary = "Update a training session",
            description = "Adds/removes exercises, edits sets and toggles them done, and/or updates the " +
                    "final note. Allowed regardless of status — a completed session can still be corrected, " +
                    "but there is no way to reopen it back to in-progress."
    )
    @PutMapping("/{id}")
    public ResponseEntity<TrainingSessionResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTrainingSessionRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        Long userId = currentUserResolver.resolveId(principal);
        return ResponseEntity.ok(trainingSessionUseCase.update(id, request, userId));
    }

    @Operation(
            summary = "Complete a training session",
            description = "Marks the session as completed, recording the final note and the elapsed " +
                    "duration since it was started. Fails if it's already completed."
    )
    @PostMapping("/{id}/complete")
    public ResponseEntity<TrainingSessionResponse> complete(
            @PathVariable Long id,
            @RequestBody(required = false) CompleteTrainingSessionRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        Long userId = currentUserResolver.resolveId(principal);
        var body = request != null ? request : new CompleteTrainingSessionRequest(null);
        return ResponseEntity.ok(trainingSessionUseCase.complete(id, body, userId));
    }

    @Operation(
            summary = "Delete a training session",
            description = "Deletes a training session. Only the owner can delete it."
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        Long userId = currentUserResolver.resolveId(principal);
        trainingSessionUseCase.delete(id, userId);
        return ResponseEntity.noContent().build();
    }
}
