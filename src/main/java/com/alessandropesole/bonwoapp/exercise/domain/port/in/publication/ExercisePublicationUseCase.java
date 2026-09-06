package com.alessandropesole.bonwoapp.exercise.domain.port.in.publication;

import com.alessandropesole.bonwoapp.exercise.application.dto.publication.CreatePublicationRequest;
import com.alessandropesole.bonwoapp.exercise.application.dto.publication.ExercisePublicationResponse;
import com.alessandropesole.bonwoapp.exercise.application.dto.publication.UpdatePublicationRequest;
import com.alessandropesole.bonwoapp.exercise.domain.model.publication.ExercisePublicationFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Set;

public interface ExercisePublicationUseCase {
    ExercisePublicationResponse create(CreatePublicationRequest request, Long authorId);

    ExercisePublicationResponse getById(Long id, Long viewerId);

    Page<ExercisePublicationResponse> listFeed(ExercisePublicationFilter filter, Long viewerId, Pageable pageable);

    Page<ExercisePublicationResponse> listMine(Long authorId, ExercisePublicationFilter filter, Pageable pageable);

    Page<ExercisePublicationResponse> listLiked(Long userId, ExercisePublicationFilter filter, Pageable pageable);

    Page<ExercisePublicationResponse> listSaved(Long userId, ExercisePublicationFilter filter, Pageable pageable);

    ExercisePublicationResponse update(Long id, UpdatePublicationRequest request, Long authorId);

    void delete(Long id, Long authorId);

    ExercisePublicationResponse like(Long id, Long userId);

    ExercisePublicationResponse unlike(Long id, Long userId);

    ExercisePublicationResponse save(Long id, Long userId);

    ExercisePublicationResponse unsave(Long id, Long userId);

    void registerUses(Long routineOwnerId, Long routineId, Set<Long> exerciseIds);
}
