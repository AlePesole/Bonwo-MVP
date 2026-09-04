package com.alessandropesole.bonwoapp.exercise.application.service.publication;

import com.alessandropesole.bonwoapp.catalog.application.service.CatalogValidator;
import com.alessandropesole.bonwoapp.catalog.domain.port.out.ActivityRepository;
import com.alessandropesole.bonwoapp.catalog.domain.port.out.EquipmentRepository;
import com.alessandropesole.bonwoapp.catalog.domain.port.out.MuscleSubGroupRepository;
import com.alessandropesole.bonwoapp.catalog.domain.port.out.TrainingGoalRepository;
import com.alessandropesole.bonwoapp.exercise.application.dto.publication.CreatePublicationRequest;
import com.alessandropesole.bonwoapp.exercise.application.dto.publication.ExercisePublicationResponse;
import com.alessandropesole.bonwoapp.exercise.application.dto.publication.UpdatePublicationRequest;
import com.alessandropesole.bonwoapp.exercise.application.service.MuscleSummaryCalculator;
import com.alessandropesole.bonwoapp.exercise.domain.exception.publication.InvalidExercisePublicationException;
import com.alessandropesole.bonwoapp.exercise.domain.model.Exercise;
import com.alessandropesole.bonwoapp.exercise.domain.model.publication.ExercisePublication;
import com.alessandropesole.bonwoapp.exercise.domain.model.Level;
import com.alessandropesole.bonwoapp.exercise.domain.model.publication.PublicationType;
import com.alessandropesole.bonwoapp.exercise.domain.model.publication.Visibility;
import com.alessandropesole.bonwoapp.exercise.domain.port.out.publication.ExercisePublicationLikeRepository;
import com.alessandropesole.bonwoapp.exercise.domain.port.out.publication.ExercisePublicationRepository;
import com.alessandropesole.bonwoapp.exercise.domain.port.out.publication.ExercisePublicationSaveRepository;
import com.alessandropesole.bonwoapp.exercise.domain.port.out.publication.ExercisePublicationUseRepository;
import com.alessandropesole.bonwoapp.exercise.domain.port.out.publication.ExercisePublicationViewRepository;
import com.alessandropesole.bonwoapp.exercise.domain.port.out.ExerciseRepository;
import com.alessandropesole.bonwoapp.media.application.service.MediaResolver;
import com.alessandropesole.bonwoapp.media.application.service.MediaService;
import com.alessandropesole.bonwoapp.shared.infrastructure.exception.ForbiddenOperationException;
import com.alessandropesole.bonwoapp.shared.infrastructure.exception.ResourceNotFoundException;
import com.alessandropesole.bonwoapp.user.domain.model.AccountStatus;
import com.alessandropesole.bonwoapp.user.domain.model.User;
import com.alessandropesole.bonwoapp.user.domain.model.UserProfile;
import com.alessandropesole.bonwoapp.user.domain.model.UserRole;
import com.alessandropesole.bonwoapp.user.domain.port.out.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExercisePublicationServiceTest {

    @Mock private ExercisePublicationRepository publicationRepository;
    @Mock private ExerciseRepository exerciseRepository;
    @Mock private ExercisePublicationLikeRepository likeRepository;
    @Mock private ExercisePublicationSaveRepository saveRepository;
    @Mock private ExercisePublicationViewRepository viewRepository;
    @Mock private ExercisePublicationUseRepository useRepository;
    @Mock private EquipmentRepository equipmentRepository;
    @Mock private ActivityRepository activityRepository;
    @Mock private TrainingGoalRepository trainingGoalRepository;
    @Mock private MuscleSubGroupRepository muscleSubGroupRepository;
    @Mock private CatalogValidator catalogValidator;
    @Mock private MediaService mediaService;
    @Mock private MediaResolver mediaResolver;
    @Mock private MuscleSummaryCalculator muscleSummaryCalculator;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private ExercisePublicationService publicationService;

    private static final Long AUTHOR_ID = 1L;
    private static final Long EXERCISE_ID = 5L;
    private static final Long PUBLICATION_ID = 10L;
    private static final Long THUMBNAIL_ID = 50L;
    private static final Long VIDEO_ID = 60L;

    private static User user(Long id, UserRole role) {
        return User.reconstitute(id, "user" + id + "@test.com", "hash", "user" + id,
                role, AccountStatus.ACTIVE, UserProfile.empty(), null);
    }

    private static CreatePublicationRequest createRequest(PublicationType type) {
        return new CreatePublicationRequest("Bench Press", Level.INTERMEDIATE, "thumb-token", "video-token",
                null, null, List.of(), Set.of(1L), Set.of(2L), Set.of(3L), type, null);
    }

    private void stubPersistence() {
        when(mediaService.claimImage("thumb-token", AUTHOR_ID)).thenReturn(THUMBNAIL_ID);
        when(mediaService.claimVideo("video-token", AUTHOR_ID)).thenReturn(VIDEO_ID);
        when(exerciseRepository.save(any(Exercise.class))).thenAnswer(inv -> {
            Exercise e = inv.getArgument(0);
            return Exercise.reconstitute(EXERCISE_ID, e.getOwnerId(), e.getTitle(), e.getLevel(),
                    e.getThumbnailId(), e.getMainVideoId(), e.getDescription(), e.getInstructions(),
                    e.getMuscles(), e.getMuscleSummary(), e.getEquipmentIds(), e.getActivityIds(),
                    e.getTrainingGoalIds(), e.getCreatedAt(), e.getPublicationId());
        });
        when(publicationRepository.save(any(ExercisePublication.class))).thenAnswer(inv -> {
            ExercisePublication p = inv.getArgument(0);
            return ExercisePublication.reconstitute(PUBLICATION_ID, p.getExerciseId(), p.getAuthorId(),
                    p.getType(), p.getVisibility(), p.getLikesCount(), p.getSavesCount(),
                    p.getViewsCount(), p.getUsesCount(), p.getPublishedAt());
        });
    }

    private ExercisePublication ownedPublication() {
        return ExercisePublication.reconstitute(PUBLICATION_ID, EXERCISE_ID, AUTHOR_ID,
                PublicationType.COMMUNITY, Visibility.PUBLIC,
                0, 0, 0, 0, null);
    }

    private Exercise ownedExercise() {
        return Exercise.reconstitute(EXERCISE_ID, AUTHOR_ID, "Bench Press", Level.INTERMEDIATE,
                null, null, null, null, List.of(), null, Set.of(), Set.of(), Set.of(), null, PUBLICATION_ID);
    }

    @Test
    void create_communityPublication_createsExerciseAndLinksThemBothWays() {
        stubPersistence();
        when(userRepository.findById(AUTHOR_ID)).thenReturn(Optional.of(user(AUTHOR_ID, UserRole.USER)));

        ExercisePublicationResponse response = publicationService.create(createRequest(PublicationType.COMMUNITY), AUTHOR_ID);

        assertThat(response.exercise().id()).isEqualTo(EXERCISE_ID);
        assertThat(response.id()).isEqualTo(PUBLICATION_ID);
        assertThat(response.exercise().publicationId()).isEqualTo(PUBLICATION_ID);
        assertThat(response.type()).isEqualTo(PublicationType.COMMUNITY);
        verify(exerciseRepository, times(2)).save(any(Exercise.class));
    }

    @Test
    void create_officialPublicationByAdmin_succeeds() {
        stubPersistence();
        when(userRepository.findById(AUTHOR_ID)).thenReturn(Optional.of(user(AUTHOR_ID, UserRole.ADMIN)));

        ExercisePublicationResponse response = publicationService.create(createRequest(PublicationType.OFFICIAL), AUTHOR_ID);

        assertThat(response.type()).isEqualTo(PublicationType.OFFICIAL);
    }

    @Test
    void create_officialPublicationByNonAdmin_throwsForbidden() {
        when(userRepository.findById(AUTHOR_ID)).thenReturn(Optional.of(user(AUTHOR_ID, UserRole.USER)));

        assertThatThrownBy(() -> publicationService.create(createRequest(PublicationType.OFFICIAL), AUTHOR_ID))
                .isInstanceOf(ForbiddenOperationException.class);
        verify(exerciseRepository, never()).save(any());
    }

    @Test
    void create_throwsWhenThumbnailIsMissing() {
        CreatePublicationRequest req = new CreatePublicationRequest("Bench Press", Level.INTERMEDIATE,
                null, "video-token", null, null, List.of(), Set.of(1L), Set.of(2L), Set.of(3L),
                PublicationType.COMMUNITY, null);

        assertThatThrownBy(() -> publicationService.create(req, AUTHOR_ID))
                .isInstanceOf(InvalidExercisePublicationException.class);
        verify(exerciseRepository, never()).save(any());
    }

    @Test
    void create_throwsWhenVideoIsMissing() {
        CreatePublicationRequest req = new CreatePublicationRequest("Bench Press", Level.INTERMEDIATE,
                "thumb-token", null, null, null, List.of(), Set.of(1L), Set.of(2L), Set.of(3L),
                PublicationType.COMMUNITY, null);

        assertThatThrownBy(() -> publicationService.create(req, AUTHOR_ID))
                .isInstanceOf(InvalidExercisePublicationException.class);
        verify(exerciseRepository, never()).save(any());
    }

    @Test
    void create_throwsWhenNoEquipment() {
        CreatePublicationRequest req = new CreatePublicationRequest("Bench Press", Level.INTERMEDIATE,
                "thumb-token", "video-token", null, null, List.of(), Set.of(), Set.of(2L), Set.of(3L),
                PublicationType.COMMUNITY, null);

        assertThatThrownBy(() -> publicationService.create(req, AUTHOR_ID))
                .isInstanceOf(InvalidExercisePublicationException.class);
        verify(exerciseRepository, never()).save(any());
    }

    @Test
    void create_throwsWhenNoActivity() {
        CreatePublicationRequest req = new CreatePublicationRequest("Bench Press", Level.INTERMEDIATE,
                "thumb-token", "video-token", null, null, List.of(), Set.of(1L), Set.of(), Set.of(3L),
                PublicationType.COMMUNITY, null);

        assertThatThrownBy(() -> publicationService.create(req, AUTHOR_ID))
                .isInstanceOf(InvalidExercisePublicationException.class);
        verify(exerciseRepository, never()).save(any());
    }

    @Test
    void create_throwsWhenNoTrainingGoal() {
        CreatePublicationRequest req = new CreatePublicationRequest("Bench Press", Level.INTERMEDIATE,
                "thumb-token", "video-token", null, null, List.of(), Set.of(1L), Set.of(2L), Set.of(),
                PublicationType.COMMUNITY, null);

        assertThatThrownBy(() -> publicationService.create(req, AUTHOR_ID))
                .isInstanceOf(InvalidExercisePublicationException.class);
        verify(exerciseRepository, never()).save(any());
    }

    @Test
    void update_throwsWhenClearingAllEquipment() {
        when(publicationRepository.findById(PUBLICATION_ID)).thenReturn(Optional.of(ownedPublication()));
        when(exerciseRepository.findById(EXERCISE_ID)).thenReturn(Optional.of(ownedExercise()));
        UpdatePublicationRequest req = new UpdatePublicationRequest(
                null, null, null, null, null, null, Set.of(), null, null, null);

        assertThatThrownBy(() -> publicationService.update(PUBLICATION_ID, req, AUTHOR_ID))
                .isInstanceOf(InvalidExercisePublicationException.class);
        verify(exerciseRepository, never()).save(any());
    }

    @Test
    void delete_removesPublication_exerciseAndEngagementRowsCascadeInTheDb() {
        when(publicationRepository.findById(PUBLICATION_ID)).thenReturn(Optional.of(ownedPublication()));
        when(exerciseRepository.findById(EXERCISE_ID)).thenReturn(Optional.of(ownedExercise()));

        publicationService.delete(PUBLICATION_ID, AUTHOR_ID);

        verify(publicationRepository).deleteById(PUBLICATION_ID);
        verify(exerciseRepository, never()).deleteById(any());
    }

    @Test
    void delete_throwsForbiddenWhenNotAuthor() {
        when(publicationRepository.findById(PUBLICATION_ID)).thenReturn(Optional.of(ownedPublication()));

        assertThatThrownBy(() -> publicationService.delete(PUBLICATION_ID, 999L))
                .isInstanceOf(ForbiddenOperationException.class);
        verify(publicationRepository, never()).deleteById(any());
    }

    @Test
    void update_throwsForbiddenWhenNotAuthor() {
        when(publicationRepository.findById(PUBLICATION_ID)).thenReturn(Optional.of(ownedPublication()));
        UpdatePublicationRequest req = new UpdatePublicationRequest(
                "New title", null, null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> publicationService.update(PUBLICATION_ID, req, 999L))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void getById_registersViewOnceThenSkipsOnSubsequentCalls() {
        when(publicationRepository.findById(PUBLICATION_ID)).thenReturn(Optional.of(ownedPublication()));
        when(exerciseRepository.findById(EXERCISE_ID)).thenReturn(Optional.of(ownedExercise()));
        when(userRepository.findById(AUTHOR_ID)).thenReturn(Optional.of(user(AUTHOR_ID, UserRole.USER)));
        when(viewRepository.exists(PUBLICATION_ID, 2L)).thenReturn(false);
        when(publicationRepository.save(any(ExercisePublication.class))).thenAnswer(inv -> inv.getArgument(0));

        ExercisePublicationResponse response = publicationService.getById(PUBLICATION_ID, 2L);

        assertThat(response.viewsCount()).isEqualTo(1);
        verify(viewRepository).add(PUBLICATION_ID, 2L);
    }

    @Test
    void getById_doesNotRegisterViewWhenAlreadyViewedByThisUser() {
        when(publicationRepository.findById(PUBLICATION_ID)).thenReturn(Optional.of(ownedPublication()));
        when(exerciseRepository.findById(EXERCISE_ID)).thenReturn(Optional.of(ownedExercise()));
        when(userRepository.findById(AUTHOR_ID)).thenReturn(Optional.of(user(AUTHOR_ID, UserRole.USER)));
        when(viewRepository.exists(PUBLICATION_ID, 2L)).thenReturn(true);

        publicationService.getById(PUBLICATION_ID, 2L);

        verify(viewRepository, never()).add(any(), any());
        verify(publicationRepository, never()).save(any());
    }

    @Test
    void like_addsLikeAndIncrementsCountWhenNotAlreadyLiked() {
        when(publicationRepository.findById(PUBLICATION_ID)).thenReturn(Optional.of(ownedPublication()));
        when(exerciseRepository.findById(EXERCISE_ID)).thenReturn(Optional.of(ownedExercise()));
        when(userRepository.findById(AUTHOR_ID)).thenReturn(Optional.of(user(AUTHOR_ID, UserRole.USER)));
        when(likeRepository.exists(PUBLICATION_ID, 2L)).thenReturn(false);
        when(publicationRepository.save(any(ExercisePublication.class))).thenAnswer(inv -> inv.getArgument(0));

        ExercisePublicationResponse response = publicationService.like(PUBLICATION_ID, 2L);

        assertThat(response.likesCount()).isEqualTo(1);
        verify(likeRepository).add(PUBLICATION_ID, 2L);
    }

    @Test
    void like_isIdempotentWhenAlreadyLiked() {
        when(publicationRepository.findById(PUBLICATION_ID)).thenReturn(Optional.of(ownedPublication()));
        when(exerciseRepository.findById(EXERCISE_ID)).thenReturn(Optional.of(ownedExercise()));
        when(userRepository.findById(AUTHOR_ID)).thenReturn(Optional.of(user(AUTHOR_ID, UserRole.USER)));
        when(likeRepository.exists(PUBLICATION_ID, 2L)).thenReturn(true);

        publicationService.like(PUBLICATION_ID, 2L);

        verify(likeRepository, never()).add(any(), any());
        verify(publicationRepository, never()).save(any());
    }

    @Test
    void unlike_removesLikeAndDecrementsCountWhenLiked() {
        when(publicationRepository.findById(PUBLICATION_ID)).thenReturn(Optional.of(ownedPublication()));
        when(exerciseRepository.findById(EXERCISE_ID)).thenReturn(Optional.of(ownedExercise()));
        when(userRepository.findById(AUTHOR_ID)).thenReturn(Optional.of(user(AUTHOR_ID, UserRole.USER)));
        when(likeRepository.exists(PUBLICATION_ID, 2L)).thenReturn(true);
        when(publicationRepository.save(any(ExercisePublication.class))).thenAnswer(inv -> inv.getArgument(0));

        publicationService.unlike(PUBLICATION_ID, 2L);

        verify(likeRepository).remove(PUBLICATION_ID, 2L);
    }

    @Test
    void save_addsSaveAndIncrementsCountWhenNotAlreadySaved() {
        when(publicationRepository.findById(PUBLICATION_ID)).thenReturn(Optional.of(ownedPublication()));
        when(exerciseRepository.findById(EXERCISE_ID)).thenReturn(Optional.of(ownedExercise()));
        when(userRepository.findById(AUTHOR_ID)).thenReturn(Optional.of(user(AUTHOR_ID, UserRole.USER)));
        when(saveRepository.exists(PUBLICATION_ID, 2L)).thenReturn(false);
        when(publicationRepository.save(any(ExercisePublication.class))).thenAnswer(inv -> inv.getArgument(0));

        ExercisePublicationResponse response = publicationService.save(PUBLICATION_ID, 2L);

        assertThat(response.savesCount()).isEqualTo(1);
        verify(saveRepository).add(PUBLICATION_ID, 2L);
    }

    @Test
    void unsave_isIdempotentWhenNotSaved() {
        when(publicationRepository.findById(PUBLICATION_ID)).thenReturn(Optional.of(ownedPublication()));
        when(exerciseRepository.findById(EXERCISE_ID)).thenReturn(Optional.of(ownedExercise()));
        when(userRepository.findById(AUTHOR_ID)).thenReturn(Optional.of(user(AUTHOR_ID, UserRole.USER)));
        when(saveRepository.exists(PUBLICATION_ID, 2L)).thenReturn(false);

        publicationService.unsave(PUBLICATION_ID, 2L);

        verify(saveRepository, never()).remove(any(), any());
        verify(publicationRepository, never()).save(any());
    }

    @Test
    void registerUses_incrementsOnceForFirstUseByThisUser() {
        when(publicationRepository.findByExerciseId(EXERCISE_ID)).thenReturn(Optional.of(ownedPublication()));
        when(useRepository.exists(PUBLICATION_ID, 3L)).thenReturn(false);

        publicationService.registerUses(3L, 100L, Set.of(EXERCISE_ID));

        verify(useRepository).add(PUBLICATION_ID, 3L, 100L);
        verify(publicationRepository).save(argThat(p -> p.getUsesCount() == 1));
    }

    @Test
    void registerUses_doesNotIncrementAgainForTheSameUser() {
        when(publicationRepository.findByExerciseId(EXERCISE_ID)).thenReturn(Optional.of(ownedPublication()));
        when(useRepository.exists(PUBLICATION_ID, 3L)).thenReturn(true);

        publicationService.registerUses(3L, 100L, Set.of(EXERCISE_ID));

        verify(useRepository, never()).add(any(), any(), any());
        verify(publicationRepository, never()).save(any());
    }

    @Test
    void registerUses_ignoresExercisesWithoutAPublication() {
        when(publicationRepository.findByExerciseId(EXERCISE_ID)).thenReturn(Optional.empty());

        publicationService.registerUses(3L, 100L, Set.of(EXERCISE_ID));

        verify(useRepository, never()).add(any(), any(), any());
        verify(publicationRepository, never()).save(any());
    }

    @Test
    void getById_throwsNotFoundWhenMissing() {
        when(publicationRepository.findById(PUBLICATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> publicationService.getById(PUBLICATION_ID, AUTHOR_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
