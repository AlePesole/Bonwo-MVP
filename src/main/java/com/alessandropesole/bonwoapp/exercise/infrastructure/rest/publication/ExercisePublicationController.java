package com.alessandropesole.bonwoapp.exercise.infrastructure.rest.publication;

import com.alessandropesole.bonwoapp.exercise.application.dto.publication.CreatePublicationRequest;
import com.alessandropesole.bonwoapp.exercise.application.dto.publication.ExercisePublicationResponse;
import com.alessandropesole.bonwoapp.exercise.application.dto.publication.UpdatePublicationRequest;
import com.alessandropesole.bonwoapp.exercise.domain.model.publication.ExercisePublicationFilter;
import com.alessandropesole.bonwoapp.exercise.domain.model.publication.PublicationSort;
import com.alessandropesole.bonwoapp.exercise.domain.model.publication.PublicationType;
import com.alessandropesole.bonwoapp.exercise.domain.port.in.publication.ExercisePublicationUseCase;
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
@RequestMapping("/exercise-publications")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class ExercisePublicationController {

    private final ExercisePublicationUseCase publicationUseCase;
    private final CurrentUserResolver currentUserResolver;

    @Operation(
            summary = "Create an exercise publication",
            description = "Creates a brand-new exercise and publishes it to the community in one step. " +
                    "OFFICIAL publications can only be created by admins. A thumbnail, a video, and at " +
                    "least one equipment, activity and training goal are mandatory."
    )
    @PostMapping
    public ResponseEntity<ExercisePublicationResponse> create(
            @Valid @RequestBody CreatePublicationRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        Long userId = currentUserResolver.resolveId(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(publicationUseCase.create(request, userId));
    }

    @Operation(
            summary = "Browse the public feed",
            description = "Returns publications visible to everyone (PUBLIC), from any author. Filterable by " +
                    "muscle group/sub-group, equipment, activity, training goal and type (COMMUNITY/OFFICIAL) " +
                    "so the front can build separate community/official sections from the same endpoint. " +
                    "sort defaults to RECENT; MOST_LIKED/MOST_VIEWED/MOST_USED order by popularity instead. " +
                    "title matches case-insensitively against the underlying exercise's title."
    )
    @GetMapping
    public ResponseEntity<Page<ExercisePublicationResponse>> listFeed(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(required = false) Long muscleGroupId,
            @RequestParam(required = false) Long muscleSubGroupId,
            @RequestParam(required = false) Set<Long> equipmentIds,
            @RequestParam(required = false) Set<Long> activityIds,
            @RequestParam(required = false) Set<Long> trainingGoalIds,
            @RequestParam(required = false) PublicationType type,
            @RequestParam(required = false) PublicationSort sort,
            @RequestParam(required = false) String title,
            @PageableDefault(size = 20) Pageable pageable) {
        Long userId = currentUserResolver.resolveId(principal);
        var filter = new ExercisePublicationFilter(muscleGroupId, muscleSubGroupId,
                equipmentIds, activityIds, trainingGoalIds, type, sort, title);
        return ResponseEntity.ok(publicationUseCase.listFeed(filter, userId, pageable));
    }

    @Operation(
            summary = "List my publications",
            description = "Returns the caller's own publications, regardless of visibility. Same filters as " +
                    "the public feed."
    )
    @GetMapping("/mine")
    public ResponseEntity<Page<ExercisePublicationResponse>> listMine(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(required = false) Long muscleGroupId,
            @RequestParam(required = false) Long muscleSubGroupId,
            @RequestParam(required = false) Set<Long> equipmentIds,
            @RequestParam(required = false) Set<Long> activityIds,
            @RequestParam(required = false) Set<Long> trainingGoalIds,
            @RequestParam(required = false) PublicationType type,
            @RequestParam(required = false) PublicationSort sort,
            @RequestParam(required = false) String title,
            @PageableDefault(size = 20) Pageable pageable) {
        Long userId = currentUserResolver.resolveId(principal);
        var filter = new ExercisePublicationFilter(muscleGroupId, muscleSubGroupId,
                equipmentIds, activityIds, trainingGoalIds, type, sort, title);
        return ResponseEntity.ok(publicationUseCase.listMine(userId, filter, pageable));
    }

    @Operation(
            summary = "List my liked publications",
            description = "Returns the publications the caller has liked. Same filters as the public feed."
    )
    @GetMapping("/liked")
    public ResponseEntity<Page<ExercisePublicationResponse>> listLiked(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(required = false) Long muscleGroupId,
            @RequestParam(required = false) Long muscleSubGroupId,
            @RequestParam(required = false) Set<Long> equipmentIds,
            @RequestParam(required = false) Set<Long> activityIds,
            @RequestParam(required = false) Set<Long> trainingGoalIds,
            @RequestParam(required = false) PublicationType type,
            @RequestParam(required = false) PublicationSort sort,
            @RequestParam(required = false) String title,
            @PageableDefault(size = 20) Pageable pageable) {
        Long userId = currentUserResolver.resolveId(principal);
        var filter = new ExercisePublicationFilter(muscleGroupId, muscleSubGroupId,
                equipmentIds, activityIds, trainingGoalIds, type, sort, title);
        return ResponseEntity.ok(publicationUseCase.listLiked(userId, filter, pageable));
    }

    @Operation(
            summary = "List my saved publications",
            description = "Returns the publications the caller has saved. Same filters as the public feed."
    )
    @GetMapping("/saved")
    public ResponseEntity<Page<ExercisePublicationResponse>> listSaved(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(required = false) Long muscleGroupId,
            @RequestParam(required = false) Long muscleSubGroupId,
            @RequestParam(required = false) Set<Long> equipmentIds,
            @RequestParam(required = false) Set<Long> activityIds,
            @RequestParam(required = false) Set<Long> trainingGoalIds,
            @RequestParam(required = false) PublicationType type,
            @RequestParam(required = false) PublicationSort sort,
            @RequestParam(required = false) String title,
            @PageableDefault(size = 20) Pageable pageable) {
        Long userId = currentUserResolver.resolveId(principal);
        var filter = new ExercisePublicationFilter(muscleGroupId, muscleSubGroupId,
                equipmentIds, activityIds, trainingGoalIds, type, sort, title);
        return ResponseEntity.ok(publicationUseCase.listSaved(userId, filter, pageable));
    }

    @Operation(
            summary = "Get a publication",
            description = "Returns a publication by id and registers a view for the caller, once per user."
    )
    @GetMapping("/{id}")
    public ResponseEntity<ExercisePublicationResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        Long userId = currentUserResolver.resolveId(principal);
        return ResponseEntity.ok(publicationUseCase.getById(id, userId));
    }

    @Operation(
            summary = "Update a publication",
            description = "Partially updates the underlying exercise and visibility. Only the author can update it. " +
                    "The video is immutable once the publication is created — it's not part of this request. " +
                    "The thumbnail can be replaced but never cleared, and equipment/activities/training goals " +
                    "can't be updated to an empty set."
    )
    @PutMapping("/{id}")
    public ResponseEntity<ExercisePublicationResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePublicationRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        Long userId = currentUserResolver.resolveId(principal);
        return ResponseEntity.ok(publicationUseCase.update(id, request, userId));
    }

    @Operation(
            summary = "Delete a publication",
            description = "Deletes the publication and the exercise it owns. Only the author can delete it."
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        Long userId = currentUserResolver.resolveId(principal);
        publicationUseCase.delete(id, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Like a publication")
    @PostMapping("/{id}/like")
    public ResponseEntity<ExercisePublicationResponse> like(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        Long userId = currentUserResolver.resolveId(principal);
        return ResponseEntity.ok(publicationUseCase.like(id, userId));
    }

    @Operation(summary = "Unlike a publication")
    @DeleteMapping("/{id}/like")
    public ResponseEntity<ExercisePublicationResponse> unlike(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        Long userId = currentUserResolver.resolveId(principal);
        return ResponseEntity.ok(publicationUseCase.unlike(id, userId));
    }

    @Operation(summary = "Save a publication")
    @PostMapping("/{id}/save")
    public ResponseEntity<ExercisePublicationResponse> save(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        Long userId = currentUserResolver.resolveId(principal);
        return ResponseEntity.ok(publicationUseCase.save(id, userId));
    }

    @Operation(summary = "Unsave a publication")
    @DeleteMapping("/{id}/save")
    public ResponseEntity<ExercisePublicationResponse> unsave(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        Long userId = currentUserResolver.resolveId(principal);
        return ResponseEntity.ok(publicationUseCase.unsave(id, userId));
    }
}
