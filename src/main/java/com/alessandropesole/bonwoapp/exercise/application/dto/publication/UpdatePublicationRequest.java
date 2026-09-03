package com.alessandropesole.bonwoapp.exercise.application.dto.publication;

import com.alessandropesole.bonwoapp.exercise.application.dto.MuscleEntryDto;
import com.alessandropesole.bonwoapp.exercise.domain.model.Level;
import com.alessandropesole.bonwoapp.exercise.domain.model.publication.Visibility;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;

public record UpdatePublicationRequest(
        @Size(max = 200) String title,
        Level level,
        String thumbnailUploadToken,
        String description,
        String instructions,
        @Valid List<MuscleEntryDto> muscles,
        Set<Long> equipmentIds,
        Set<Long> activityIds,
        Set<Long> trainingGoalIds,
        Visibility visibility
) {}
