package com.alessandropesole.bonwoapp.exercise.application.service.publication;

import com.alessandropesole.bonwoapp.exercise.domain.model.Exercise;
import com.alessandropesole.bonwoapp.exercise.domain.model.publication.ExercisePublication;
import com.alessandropesole.bonwoapp.exercise.domain.port.out.publication.ExercisePublicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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

    /** Batched version of {@link #isVisible} — resolves every non-owned exercise's publication in
     *  one query instead of one per exercise. Result always has an entry for every input exercise. */
    public Map<Long, Boolean> isVisibleBulk(List<Exercise> exercises, Long viewerId) {
        Set<Long> publicationIds = exercises.stream()
                .filter(e -> !e.isOwnedBy(viewerId))
                .map(Exercise::getPublicationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, ExercisePublication> publications = publicationIds.isEmpty() ? Map.of()
                : publicationRepository.findAllById(publicationIds).stream()
                    .collect(Collectors.toMap(ExercisePublication::getId, p -> p));

        return exercises.stream().collect(Collectors.toMap(Exercise::getId, e -> {
            if (e.isOwnedBy(viewerId)) return true;
            if (e.getPublicationId() == null) return false;
            ExercisePublication p = publications.get(e.getPublicationId());
            return p != null && p.isVisibleTo(viewerId);
        }));
    }
}
