package com.alessandropesole.bonwoapp.catalog.infrastructure.rest;

import com.alessandropesole.bonwoapp.catalog.application.dto.*;
import com.alessandropesole.bonwoapp.catalog.domain.port.in.ActivityUseCase;
import com.alessandropesole.bonwoapp.catalog.domain.port.in.EquipmentUseCase;
import com.alessandropesole.bonwoapp.catalog.domain.port.in.TrainingGoalUseCase;
import com.alessandropesole.bonwoapp.user.application.service.CurrentUserResolver;
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
@RequestMapping("/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final EquipmentUseCase equipmentUseCase;
    private final ActivityUseCase activityUseCase;
    private final TrainingGoalUseCase trainingGoalUseCase;
    private final CurrentUserResolver currentUserResolver;

    // -- Equipment --

    @GetMapping("/equipment")
    public ResponseEntity<List<EquipmentResponse>> listEquipment() {
        return ResponseEntity.ok(equipmentUseCase.listAll());
    }

    @PostMapping("/equipment")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EquipmentResponse> createEquipment(
            @Valid @RequestBody EquipmentRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(equipmentUseCase.create(request, currentUserResolver.resolveId(principal)));
    }

    @PutMapping("/equipment/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EquipmentResponse> updateEquipment(
            @PathVariable Long id,
            @Valid @RequestBody EquipmentRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(
                equipmentUseCase.update(id, request, currentUserResolver.resolveId(principal)));
    }

    @DeleteMapping("/equipment/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteEquipment(@PathVariable Long id) {
        equipmentUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }

    // -- Activity --

    @GetMapping("/activities")
    public ResponseEntity<List<ActivityResponse>> listActivities() {
        return ResponseEntity.ok(activityUseCase.listAll());
    }

    @PostMapping("/activities")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ActivityResponse> createActivity(
            @Valid @RequestBody ActivityRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(activityUseCase.create(request, currentUserResolver.resolveId(principal)));
    }

    @PutMapping("/activities/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ActivityResponse> updateActivity(
            @PathVariable Long id,
            @Valid @RequestBody ActivityRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(
                activityUseCase.update(id, request, currentUserResolver.resolveId(principal)));
    }

    @DeleteMapping("/activities/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteActivity(@PathVariable Long id) {
        activityUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }

    // -- TrainingGoal --

    @GetMapping("/training-goals")
    public ResponseEntity<List<TrainingGoalResponse>> listTrainingGoals() {
        return ResponseEntity.ok(trainingGoalUseCase.listAll());
    }

    @PostMapping("/training-goals")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TrainingGoalResponse> createTrainingGoal(
            @Valid @RequestBody TrainingGoalRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(trainingGoalUseCase.create(request, currentUserResolver.resolveId(principal)));
    }

    @PutMapping("/training-goals/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TrainingGoalResponse> updateTrainingGoal(
            @PathVariable Long id,
            @Valid @RequestBody TrainingGoalRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(
                trainingGoalUseCase.update(id, request, currentUserResolver.resolveId(principal)));
    }

    @DeleteMapping("/training-goals/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTrainingGoal(@PathVariable Long id) {
        trainingGoalUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }
}
