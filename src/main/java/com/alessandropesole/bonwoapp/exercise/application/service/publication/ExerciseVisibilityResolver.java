package com.alessandropesole.bonwoapp.exercise.application.service.publication;

import com.alessandropesole.bonwoapp.exercise.domain.model.Exercise;
import com.alessandropesole.bonwoapp.exercise.domain.port.out.publication.ExercisePublicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExerciseVisibilityResolver {

    private final ExercisePublicationRepository publicationRepository;

    public boolean isVisible(Exercise exercise, Long viewerId) {
        if (exercise.isOwnedBy(viewerId)) return true;
        if (exercise.getPublicationId() == null) return false;
        return publicationRepository.findById(exercise.getPublicationId())
                .map(p -> p.isVisibleTo(viewerId))
                .orElse(false);
    }
}
