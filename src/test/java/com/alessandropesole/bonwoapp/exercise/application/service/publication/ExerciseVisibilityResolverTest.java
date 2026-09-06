package com.alessandropesole.bonwoapp.exercise.application.service.publication;

import com.alessandropesole.bonwoapp.exercise.domain.model.Exercise;
import com.alessandropesole.bonwoapp.exercise.domain.model.publication.ExercisePublication;
import com.alessandropesole.bonwoapp.exercise.domain.model.Level;
import com.alessandropesole.bonwoapp.exercise.domain.model.publication.PublicationType;
import com.alessandropesole.bonwoapp.exercise.domain.model.publication.Visibility;
import com.alessandropesole.bonwoapp.exercise.domain.port.out.publication.ExercisePublicationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExerciseVisibilityResolverTest {

    @Mock private ExercisePublicationRepository publicationRepository;

    @InjectMocks
    private ExerciseVisibilityResolver visibilityResolver;

    private static final Long OWNER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long PUBLICATION_ID = 10L;

    private static Exercise privateExercise() {
        return Exercise.reconstitute(1L, OWNER_ID, "Bench Press", Level.INTERMEDIATE,
                null, null, null, null, List.of(), null, Set.of(), Set.of(), Set.of(), null, null);
    }

    private static Exercise publishedExercise() {
        return Exercise.reconstitute(1L, OWNER_ID, "Bench Press", Level.INTERMEDIATE,
                null, null, null, null, List.of(), null, Set.of(), Set.of(), Set.of(), null, PUBLICATION_ID);
    }

    @Test
    void isVisible_trueWhenViewerIsTheOwner() {
        Exercise exercise = privateExercise();

        assertThat(visibilityResolver.isVisible(exercise, OWNER_ID)).isTrue();
    }

    @Test
    void isVisible_falseWhenNotOwnerAndNotPublished() {
        Exercise exercise = privateExercise();

        assertThat(visibilityResolver.isVisible(exercise, OTHER_USER_ID)).isFalse();
    }

    @Test
    void isVisible_trueWhenNotOwnerButPublicationIsPublic() {
        Exercise exercise = publishedExercise();
        ExercisePublication publication = ExercisePublication.reconstitute(
                PUBLICATION_ID, exercise.getId(), OWNER_ID, PublicationType.COMMUNITY, Visibility.PUBLIC,
                0, 0, 0, 0, null);
        when(publicationRepository.findById(PUBLICATION_ID)).thenReturn(Optional.of(publication));

        assertThat(visibilityResolver.isVisible(exercise, OTHER_USER_ID)).isTrue();
    }

    @Test
    void isVisible_falseWhenPublicationIdPointsToNothing() {
        Exercise exercise = publishedExercise();
        when(publicationRepository.findById(PUBLICATION_ID)).thenReturn(Optional.empty());

        assertThat(visibilityResolver.isVisible(exercise, OTHER_USER_ID)).isFalse();
    }

    @Test
    void isVisibleBulk_trueForOwnedExerciseWithoutPublicationLookup() {
        Exercise exercise = privateExercise();

        Map<Long, Boolean> result = visibilityResolver.isVisibleBulk(List.of(exercise), OWNER_ID);

        assertThat(result).containsEntry(exercise.getId(), true);
        verify(publicationRepository, never()).findAllById(any());
    }

    @Test
    void isVisibleBulk_falseForNonOwnedExerciseWithoutPublication() {
        Exercise exercise = privateExercise();

        Map<Long, Boolean> result = visibilityResolver.isVisibleBulk(List.of(exercise), OTHER_USER_ID);

        assertThat(result).containsEntry(exercise.getId(), false);
        verify(publicationRepository, never()).findAllById(any());
    }

    @Test
    void isVisibleBulk_trueForNonOwnedExerciseWithVisiblePublication() {
        Exercise exercise = publishedExercise();
        ExercisePublication publication = ExercisePublication.reconstitute(
                PUBLICATION_ID, exercise.getId(), OWNER_ID, PublicationType.COMMUNITY, Visibility.PUBLIC,
                0, 0, 0, 0, null);
        when(publicationRepository.findAllById(Set.of(PUBLICATION_ID))).thenReturn(List.of(publication));

        Map<Long, Boolean> result = visibilityResolver.isVisibleBulk(List.of(exercise), OTHER_USER_ID);

        assertThat(result).containsEntry(exercise.getId(), true);
    }

    @Test
    void isVisibleBulk_batchesPublicationLookupAcrossSharedPublicationId() {
        Exercise a = Exercise.reconstitute(1L, OWNER_ID, "Bench Press", Level.INTERMEDIATE,
                null, null, null, null, List.of(), null, Set.of(), Set.of(), Set.of(), null, PUBLICATION_ID);
        Exercise b = Exercise.reconstitute(2L, OWNER_ID, "Incline Bench Press", Level.INTERMEDIATE,
                null, null, null, null, List.of(), null, Set.of(), Set.of(), Set.of(), null, PUBLICATION_ID);
        ExercisePublication publication = ExercisePublication.reconstitute(
                PUBLICATION_ID, a.getId(), OWNER_ID, PublicationType.COMMUNITY, Visibility.PUBLIC,
                0, 0, 0, 0, null);
        when(publicationRepository.findAllById(Set.of(PUBLICATION_ID))).thenReturn(List.of(publication));

        visibilityResolver.isVisibleBulk(List.of(a, b), OTHER_USER_ID);

        verify(publicationRepository, times(1)).findAllById(any());
    }
}
