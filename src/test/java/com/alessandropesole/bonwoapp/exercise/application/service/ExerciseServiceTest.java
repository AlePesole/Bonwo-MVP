package com.alessandropesole.bonwoapp.exercise.application.service;

import com.alessandropesole.bonwoapp.catalog.application.service.CatalogValidator;
import com.alessandropesole.bonwoapp.catalog.domain.model.MuscleSubGroup;
import com.alessandropesole.bonwoapp.catalog.domain.port.out.ActivityRepository;
import com.alessandropesole.bonwoapp.catalog.domain.port.out.EquipmentRepository;
import com.alessandropesole.bonwoapp.catalog.domain.port.out.MuscleSubGroupRepository;
import com.alessandropesole.bonwoapp.catalog.domain.port.out.TrainingGoalRepository;
import com.alessandropesole.bonwoapp.exercise.application.dto.CreateExerciseRequest;
import com.alessandropesole.bonwoapp.exercise.application.dto.ExerciseResponse;
import com.alessandropesole.bonwoapp.exercise.application.dto.UpdateExerciseRequest;
import com.alessandropesole.bonwoapp.exercise.domain.model.Exercise;
import com.alessandropesole.bonwoapp.exercise.domain.model.ExerciseFilter;
import com.alessandropesole.bonwoapp.exercise.domain.model.Level;
import com.alessandropesole.bonwoapp.exercise.domain.port.out.ExerciseRepository;
import com.alessandropesole.bonwoapp.media.application.service.MediaResolver;
import com.alessandropesole.bonwoapp.media.application.service.MediaService;
import com.alessandropesole.bonwoapp.shared.infrastructure.exception.ForbiddenOperationException;
import com.alessandropesole.bonwoapp.shared.infrastructure.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExerciseServiceTest {

    @Mock private ExerciseRepository exerciseRepository;
    @Mock private EquipmentRepository equipmentRepository;
    @Mock private ActivityRepository activityRepository;
    @Mock private TrainingGoalRepository trainingGoalRepository;
    @Mock private MuscleSubGroupRepository muscleSubGroupRepository;
    @Mock private CatalogValidator catalogValidator;
    @Mock private MediaService mediaService;
    @Mock private MediaResolver mediaResolver;
    @Mock private MuscleSummaryCalculator muscleSummaryCalculator;

    @InjectMocks
    private ExerciseService exerciseService;

    private static final Long OWNER_ID = 1L;

    @Test
    void create_savesExerciseAndReturnsResponse() {
        CreateExerciseRequest req = new CreateExerciseRequest(
                "Bench Press", Level.INTERMEDIATE, null, null, null, null,
                List.of(), Set.of(), Set.of(), Set.of());
        when(exerciseRepository.save(any(Exercise.class))).thenAnswer(inv -> inv.getArgument(0));

        ExerciseResponse response = exerciseService.create(req, OWNER_ID);

        assertThat(response.title()).isEqualTo("Bench Press");
        assertThat(response.ownerId()).isEqualTo(OWNER_ID);
        verify(catalogValidator).validate(Set.of(), Set.of(), Set.of());
        verify(exerciseRepository).save(any(Exercise.class));
    }

    @Test
    void create_claimsThumbnailAndVideoWhenTokensProvided() {
        CreateExerciseRequest req = new CreateExerciseRequest(
                "Squat", Level.BEGINNER, "thumb-token", "video-token", null, null,
                List.of(), Set.of(), Set.of(), Set.of());
        when(mediaService.claimImage("thumb-token", OWNER_ID)).thenReturn(10L);
        when(mediaService.claimVideo("video-token", OWNER_ID)).thenReturn(20L);
        when(exerciseRepository.save(any(Exercise.class))).thenAnswer(inv -> inv.getArgument(0));

        exerciseService.create(req, OWNER_ID);

        verify(mediaService).claimImage("thumb-token", OWNER_ID);
        verify(mediaService).claimVideo("video-token", OWNER_ID);
    }

    @Test
    void getById_returnsResponseWhenOwned() {
        Exercise exercise = Exercise.reconstitute(1L, OWNER_ID, "Deadlift", Level.ADVANCED,
                null, null, null, null, List.of(), null, Set.of(), Set.of(), Set.of(), null);
        when(exerciseRepository.findById(1L)).thenReturn(Optional.of(exercise));

        ExerciseResponse response = exerciseService.getById(1L, OWNER_ID);

        assertThat(response.title()).isEqualTo("Deadlift");
    }

    @Test
    void getById_throwsNotFoundWhenMissing() {
        when(exerciseRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> exerciseService.getById(1L, OWNER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getById_throwsForbiddenWhenNotOwner() {
        Exercise exercise = Exercise.reconstitute(1L, 999L, "Deadlift", Level.ADVANCED,
                null, null, null, null, List.of(), null, Set.of(), Set.of(), Set.of(), null);
        when(exerciseRepository.findById(1L)).thenReturn(Optional.of(exercise));

        assertThatThrownBy(() -> exerciseService.getById(1L, OWNER_ID))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
void update_removesThumbnailWithoutPhysicallyDeletingTheImage() {
        Exercise exercise = Exercise.reconstitute(1L, OWNER_ID, "Bench Press", Level.INTERMEDIATE,
                50L, null, null, null, List.of(), null, Set.of(), Set.of(), Set.of(), null);
        when(exerciseRepository.findById(1L)).thenReturn(Optional.of(exercise));
        when(exerciseRepository.save(any(Exercise.class))).thenAnswer(inv -> inv.getArgument(0));
        UpdateExerciseRequest req = new UpdateExerciseRequest(
                null, null, null, true, null, false, null, null, null, null, null, null);

        ExerciseResponse response = exerciseService.update(1L, req, OWNER_ID);

        assertThat(response.thumbnail()).isNull();
        verify(mediaService, never()).deleteImageIfOwner(any(), any());
    }

    @Test
    void update_removesMainVideoAndDeletesOldVideoWhenRequested() {
        Exercise exercise = Exercise.reconstitute(1L, OWNER_ID, "Bench Press", Level.INTERMEDIATE,
                null, 60L, null, null, List.of(), null, Set.of(), Set.of(), Set.of(), null);
        when(exerciseRepository.findById(1L)).thenReturn(Optional.of(exercise));
        when(exerciseRepository.save(any(Exercise.class))).thenAnswer(inv -> inv.getArgument(0));
        UpdateExerciseRequest req = new UpdateExerciseRequest(
                null, null, null, false, null, true, null, null, null, null, null, null);

        exerciseService.update(1L, req, OWNER_ID);

        verify(mediaService).deleteVideoIfOwner(60L, OWNER_ID);
    }

    @Test
    void update_throwsForbiddenWhenNotOwner() {
        Exercise exercise = Exercise.reconstitute(1L, 999L, "Bench Press", Level.INTERMEDIATE,
                null, null, null, null, List.of(), null, Set.of(), Set.of(), Set.of(), null);
        when(exerciseRepository.findById(1L)).thenReturn(Optional.of(exercise));
        UpdateExerciseRequest req = new UpdateExerciseRequest(
                "New title", null, null, false, null, false, null, null, null, null, null, null);

        assertThatThrownBy(() -> exerciseService.update(1L, req, OWNER_ID))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void delete_deletesVideoButNotThumbnailImage() {
        Exercise exercise = Exercise.reconstitute(1L, OWNER_ID, "Bench Press", Level.INTERMEDIATE,
                50L, 60L, null, null, List.of(), null, Set.of(), Set.of(), Set.of(), null);
        when(exerciseRepository.findById(1L)).thenReturn(Optional.of(exercise));

        exerciseService.delete(1L, OWNER_ID);

        verify(mediaService).deleteVideoIfOwner(60L, OWNER_ID);
        verify(mediaService, never()).deleteImageIfOwner(any(), any());
        verify(exerciseRepository).deleteById(1L);
    }

    @Test
    void listMine_resolvesMuscleGroupIdToItsSubGroupIds() {
        ExerciseFilter filter = new ExerciseFilter(1L, null, null, null, null);
        var sub1 = MuscleSubGroup.reconstitute(4L, 1L, "Middle Chest", null, "path", null, null);
        var sub2 = MuscleSubGroup.reconstitute(7L, 1L, "Upper Chest", null, "path", null, null);
        when(muscleSubGroupRepository.findByGroupId(1L)).thenReturn(List.of(sub1, sub2));
        when(exerciseRepository.findByOwner(eq(OWNER_ID), eq(Set.of(4L, 7L)), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        exerciseService.listMine(OWNER_ID, filter, PageRequest.of(0, 10));

        verify(exerciseRepository).findByOwner(eq(OWNER_ID), eq(Set.of(4L, 7L)), any(), any(), any(), any());
    }

    @Test
    void listMine_usesExplicitSubGroupIdOverGroupId() {
        ExerciseFilter filter = new ExerciseFilter(1L, 4L, null, null, null);
        when(exerciseRepository.findByOwner(eq(OWNER_ID), eq(Set.of(4L)), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        exerciseService.listMine(OWNER_ID, filter, PageRequest.of(0, 10));

        verifyNoInteractions(muscleSubGroupRepository);
        verify(exerciseRepository).findByOwner(eq(OWNER_ID), eq(Set.of(4L)), any(), any(), any(), any());
    }
}
