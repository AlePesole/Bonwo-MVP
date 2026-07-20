package com.alessandropesole.bonwoapp.exercise.infrastructure.rest;

import com.alessandropesole.bonwoapp.exercise.application.dto.CreateExerciseRequest;
import com.alessandropesole.bonwoapp.exercise.application.dto.ExerciseResponse;
import com.alessandropesole.bonwoapp.exercise.application.dto.UpdateExerciseRequest;
import com.alessandropesole.bonwoapp.exercise.domain.model.ExerciseFilter;
import com.alessandropesole.bonwoapp.exercise.domain.port.in.ExerciseUseCase;
import com.alessandropesole.bonwoapp.user.application.service.CurrentUserResolver;
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

    @GetMapping("/{id}")
    public ResponseEntity<ExerciseResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        Long userId = currentUserResolver.resolveId(principal);
        return ResponseEntity.ok(exerciseUseCase.getById(id, userId));
    }

    @PostMapping
    public ResponseEntity<ExerciseResponse> create(
            @Valid @RequestBody CreateExerciseRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        Long userId = currentUserResolver.resolveId(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(exerciseUseCase.create(request, userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExerciseResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateExerciseRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        Long userId = currentUserResolver.resolveId(principal);
        return ResponseEntity.ok(exerciseUseCase.update(id, request, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        Long userId = currentUserResolver.resolveId(principal);
        exerciseUseCase.delete(id, userId);
        return ResponseEntity.noContent().build();
    }
}
