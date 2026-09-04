package com.alessandropesole.bonwoapp.routine.application.service;

import com.alessandropesole.bonwoapp.catalog.application.service.CatalogValidator;
import com.alessandropesole.bonwoapp.catalog.domain.port.out.ActivityRepository;
import com.alessandropesole.bonwoapp.catalog.domain.port.out.EquipmentRepository;
import com.alessandropesole.bonwoapp.catalog.domain.port.out.TrainingGoalRepository;
import com.alessandropesole.bonwoapp.exercise.application.dto.ExerciseResponse;
import com.alessandropesole.bonwoapp.exercise.application.service.publication.ExerciseVisibilityResolver;
import com.alessandropesole.bonwoapp.exercise.application.service.MuscleSummaryCalculator;
import com.alessandropesole.bonwoapp.exercise.domain.model.Exercise;
import com.alessandropesole.bonwoapp.exercise.domain.model.Level;
import com.alessandropesole.bonwoapp.exercise.domain.model.MuscleSummary;
import com.alessandropesole.bonwoapp.exercise.domain.port.in.publication.ExercisePublicationUseCase;
import com.alessandropesole.bonwoapp.exercise.domain.port.in.ExerciseUseCase;
import com.alessandropesole.bonwoapp.exercise.domain.port.out.ExerciseRepository;
import com.alessandropesole.bonwoapp.media.application.service.MediaResolver;
import com.alessandropesole.bonwoapp.media.application.service.MediaService;
import com.alessandropesole.bonwoapp.media.domain.exception.MediaNotOwnedException;
import com.alessandropesole.bonwoapp.routine.application.dto.CreateRoutineRequest;
import com.alessandropesole.bonwoapp.routine.application.dto.ExerciseSlotDto;
import com.alessandropesole.bonwoapp.routine.application.dto.RoutineResponse;
import com.alessandropesole.bonwoapp.routine.application.dto.SetConfigDto;
import com.alessandropesole.bonwoapp.routine.domain.model.ExerciseSlot;
import com.alessandropesole.bonwoapp.routine.domain.model.Routine;
import com.alessandropesole.bonwoapp.routine.domain.model.SetConfig;
import com.alessandropesole.bonwoapp.routine.domain.model.SetType;
import com.alessandropesole.bonwoapp.routine.domain.port.out.RoutineRepository;
import com.alessandropesole.bonwoapp.shared.infrastructure.exception.ForbiddenOperationException;
import com.alessandropesole.bonwoapp.shared.infrastructure.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoutineServiceTest {

    @Mock private RoutineRepository routineRepository;
    @Mock private EquipmentRepository equipmentRepository;
    @Mock private ActivityRepository activityRepository;
    @Mock private TrainingGoalRepository trainingGoalRepository;
    @Mock private CatalogValidator catalogValidator;
    @Mock private MediaService mediaService;
    @Mock private MediaResolver mediaResolver;
    @Mock private MuscleSummaryCalculator muscleSummaryCalculator;
    @Mock private ExerciseUseCase exerciseUseCase;
    @Mock private ExerciseRepository exerciseRepository;
    @Mock private ExercisePublicationUseCase exercisePublicationUseCase;
    @Mock private ExerciseVisibilityResolver exerciseVisibilityResolver;

    @InjectMocks
    private RoutineService routineService;

    private static final Long OWNER_ID = 1L;
    private static final Long EXERCISE_ID = 5L;

    private static Exercise ownedExercise() {
        return Exercise.reconstitute(EXERCISE_ID, OWNER_ID, "Bench Press", Level.INTERMEDIATE,
                null, null, null, null, List.of(), null, Set.of(), Set.of(), Set.of(), null, null);
    }

    private static Exercise publishedExerciseOwnedBySomeoneElse() {
        return Exercise.reconstitute(EXERCISE_ID, 999L, "Bench Press", Level.INTERMEDIATE,
                null, null, null, null, List.of(), null, Set.of(), Set.of(), Set.of(), null, 77L);
    }

    private static Exercise privateExerciseOwnedBySomeoneElse() {
        return Exercise.reconstitute(EXERCISE_ID, 999L, "Bench Press", Level.INTERMEDIATE,
                null, null, null, null, List.of(), null, Set.of(), Set.of(), Set.of(), null, null);
    }

    private static ExerciseResponse exerciseResponse(Map<Long, Double> muscleSummary) {
        return new ExerciseResponse(EXERCISE_ID, OWNER_ID, "Bench Press", Level.INTERMEDIATE,
                null, null, null, null, muscleSummary, List.of(), List.of(), List.of(), List.of(), null, null);
    }

    @Test
    void create_aggregatesMuscleSummaryFromExistingOwnedExercise() {
        CreateRoutineRequest req = new CreateRoutineRequest("Push Day", null, Level.INTERMEDIATE, null, null,
                List.of(new ExerciseSlotDto(EXERCISE_ID, 1,
                        List.of(new SetConfigDto(SetType.REPS, 10, null, null, null)), null)),
                null, Set.of(), Set.of(), Set.of());
        Exercise exercise = ownedExercise();
        when(exerciseRepository.findById(EXERCISE_ID)).thenReturn(Optional.of(exercise));
        when(exerciseVisibilityResolver.isVisible(exercise, OWNER_ID)).thenReturn(true);
        when(exerciseUseCase.getById(EXERCISE_ID, OWNER_ID)).thenReturn(exerciseResponse(Map.of(1L, 0.8)));
        when(muscleSummaryCalculator.aggregate(anyList())).thenReturn(MuscleSummary.of(Map.of(1L, 0.8)));
        when(routineRepository.save(any(Routine.class))).thenAnswer(inv -> inv.getArgument(0));

        RoutineResponse response = routineService.create(req, OWNER_ID);

        ArgumentCaptor<List<MuscleSummary>> captor = ArgumentCaptor.forClass(List.class);
        verify(muscleSummaryCalculator).aggregate(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(response.muscleSummary()).isEqualTo(Map.of(1L, 0.8));
    }

    @Test
    void create_aggregatesMuscleSummaryFromPublishedExerciseNotOwnedByCaller() {
        CreateRoutineRequest req = new CreateRoutineRequest("Push Day", null, Level.INTERMEDIATE, null, null,
                List.of(new ExerciseSlotDto(EXERCISE_ID, 1,
                        List.of(new SetConfigDto(SetType.REPS, 10, null, null, null)), null)),
                null, Set.of(), Set.of(), Set.of());
        Exercise exercise = publishedExerciseOwnedBySomeoneElse();
        when(exerciseRepository.findById(EXERCISE_ID)).thenReturn(Optional.of(exercise));
        when(exerciseVisibilityResolver.isVisible(exercise, OWNER_ID)).thenReturn(true);
        when(exerciseUseCase.getById(EXERCISE_ID, OWNER_ID)).thenReturn(exerciseResponse(Map.of(1L, 0.8)));
        when(muscleSummaryCalculator.aggregate(anyList())).thenReturn(MuscleSummary.of(Map.of(1L, 0.8)));
        when(routineRepository.save(any(Routine.class))).thenAnswer(inv -> inv.getArgument(0));

        RoutineResponse response = routineService.create(req, OWNER_ID);

        ArgumentCaptor<List<MuscleSummary>> captor = ArgumentCaptor.forClass(List.class);
        verify(muscleSummaryCalculator).aggregate(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(response.muscleSummary()).isEqualTo(Map.of(1L, 0.8));
        assertThat(response.slots().get(0).exercise()).isNotNull();
    }

    @Test
    void create_throwsNotFoundWhenSlotExerciseDoesNotExist() {
        CreateRoutineRequest req = new CreateRoutineRequest("Push Day", null, Level.INTERMEDIATE, null, null,
                List.of(new ExerciseSlotDto(EXERCISE_ID, 1,
                        List.of(new SetConfigDto(SetType.REPS, 10, null, null, null)), null)),
                null, Set.of(), Set.of(), Set.of());
        when(exerciseRepository.findById(EXERCISE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> routineService.create(req, OWNER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(exerciseUseCase, never()).getById(any(), any());
        verify(routineRepository, never()).save(any());
    }

    @Test
    void create_throwsForbiddenWhenSlotExerciseIsPrivateAndNotOwnedByCaller() {
        CreateRoutineRequest req = new CreateRoutineRequest("Push Day", null, Level.INTERMEDIATE, null, null,
                List.of(new ExerciseSlotDto(EXERCISE_ID, 1,
                        List.of(new SetConfigDto(SetType.REPS, 10, null, null, null)), null)),
                null, Set.of(), Set.of(), Set.of());
        Exercise exercise = privateExerciseOwnedBySomeoneElse();
        when(exerciseRepository.findById(EXERCISE_ID)).thenReturn(Optional.of(exercise));
        when(exerciseVisibilityResolver.isVisible(exercise, OWNER_ID)).thenReturn(false);

        assertThatThrownBy(() -> routineService.create(req, OWNER_ID))
                .isInstanceOf(ForbiddenOperationException.class);
        verify(exerciseUseCase, never()).getById(any(), any());
        verify(routineRepository, never()).save(any());
    }

    @Test
    void create_reusesExistingOwnedThumbnailIdWithoutClaimingAnUpload() {
        CreateRoutineRequest req = new CreateRoutineRequest("Push Day Copy", null, Level.INTERMEDIATE,
                null, 99L,
                List.of(new ExerciseSlotDto(EXERCISE_ID, 1,
                        List.of(new SetConfigDto(SetType.REPS, 10, null, null, null)), null)),
                null, Set.of(), Set.of(), Set.of());
        Exercise exercise = ownedExercise();
        when(exerciseRepository.findById(EXERCISE_ID)).thenReturn(Optional.of(exercise));
        when(exerciseVisibilityResolver.isVisible(exercise, OWNER_ID)).thenReturn(true);
        when(exerciseUseCase.getById(EXERCISE_ID, OWNER_ID)).thenReturn(exerciseResponse(Map.of()));
        when(muscleSummaryCalculator.aggregate(anyList())).thenReturn(MuscleSummary.empty());
        when(routineRepository.save(any(Routine.class))).thenAnswer(inv -> inv.getArgument(0));

        routineService.create(req, OWNER_ID);

        verify(mediaService).verifyImageOwnership(99L, OWNER_ID);
        verify(mediaService, never()).claimImage(any(), any());
    }

    @Test
    void create_prefersUploadTokenOverThumbnailIdWhenBothProvided() {
        CreateRoutineRequest req = new CreateRoutineRequest("Push Day Copy", null, Level.INTERMEDIATE,
                "fresh-token", 99L,
                List.of(new ExerciseSlotDto(EXERCISE_ID, 1,
                        List.of(new SetConfigDto(SetType.REPS, 10, null, null, null)), null)),
                null, Set.of(), Set.of(), Set.of());
        Exercise exercise = ownedExercise();
        when(exerciseRepository.findById(EXERCISE_ID)).thenReturn(Optional.of(exercise));
        when(exerciseVisibilityResolver.isVisible(exercise, OWNER_ID)).thenReturn(true);
        when(exerciseUseCase.getById(EXERCISE_ID, OWNER_ID)).thenReturn(exerciseResponse(Map.of()));
        when(muscleSummaryCalculator.aggregate(anyList())).thenReturn(MuscleSummary.empty());
        when(mediaService.claimImage("fresh-token", OWNER_ID)).thenReturn(200L);
        when(routineRepository.save(any(Routine.class))).thenAnswer(inv -> inv.getArgument(0));

        routineService.create(req, OWNER_ID);

        verify(mediaService).claimImage("fresh-token", OWNER_ID);
        verify(mediaService, never()).verifyImageOwnership(any(), any());
    }

    @Test
    void create_rejectsThumbnailIdNotOwnedByCaller() {
        CreateRoutineRequest req = new CreateRoutineRequest("Push Day Copy", null, Level.INTERMEDIATE,
                null, 99L,
                List.of(new ExerciseSlotDto(EXERCISE_ID, 1,
                        List.of(new SetConfigDto(SetType.REPS, 10, null, null, null)), null)),
                null, Set.of(), Set.of(), Set.of());
        Exercise exercise = ownedExercise();
        when(exerciseRepository.findById(EXERCISE_ID)).thenReturn(Optional.of(exercise));
        when(exerciseVisibilityResolver.isVisible(exercise, OWNER_ID)).thenReturn(true);
        when(exerciseUseCase.getById(EXERCISE_ID, OWNER_ID)).thenReturn(exerciseResponse(Map.of()));
        when(muscleSummaryCalculator.aggregate(anyList())).thenReturn(MuscleSummary.empty());
        doThrow(new MediaNotOwnedException()).when(mediaService).verifyImageOwnership(99L, OWNER_ID);

        assertThatThrownBy(() -> routineService.create(req, OWNER_ID))
                .isInstanceOf(MediaNotOwnedException.class);
        verify(routineRepository, never()).save(any());
    }

    @Test
    void getById_resolvesExistingOwnedExerciseForEachSlot() {
        ExerciseSlot slot = ExerciseSlot.reconstitute(EXERCISE_ID, 1,
                List.of(SetConfig.reps(10, null, null)), null);
        Routine routine = Routine.reconstitute(10L, OWNER_ID, "Push Day", null, Level.INTERMEDIATE,
                null, Duration.ZERO, List.of(slot), null, MuscleSummary.empty(),
                Set.of(), Set.of(), Set.of(), null, null, null);
        Exercise exercise = ownedExercise();
        when(routineRepository.findById(10L)).thenReturn(Optional.of(routine));
        when(exerciseRepository.findById(EXERCISE_ID)).thenReturn(Optional.of(exercise));
        when(exerciseVisibilityResolver.isVisible(exercise, OWNER_ID)).thenReturn(true);
        when(exerciseUseCase.getById(EXERCISE_ID, OWNER_ID)).thenReturn(exerciseResponse(Map.of()));

        RoutineResponse response = routineService.getById(10L, OWNER_ID);

        assertThat(response.slots().get(0).exercise()).isNotNull();
        assertThat(response.slots().get(0).exercise().title()).isEqualTo("Bench Press");
    }

    @Test
    void getById_returnsNullExerciseForDeletedExerciseWithoutCallingThrowingUseCase() {
        ExerciseSlot slot = ExerciseSlot.reconstitute(EXERCISE_ID, 1,
                List.of(SetConfig.reps(10, null, null)), null);
        Routine routine = Routine.reconstitute(10L, OWNER_ID, "Push Day", null, Level.INTERMEDIATE,
                null, Duration.ZERO, List.of(slot), null, MuscleSummary.empty(),
                Set.of(), Set.of(), Set.of(), null, null, null);
        when(routineRepository.findById(10L)).thenReturn(Optional.of(routine));
        when(exerciseRepository.findById(EXERCISE_ID)).thenReturn(Optional.empty());

        RoutineResponse response = routineService.getById(10L, OWNER_ID);

        assertThat(response.slots().get(0).exercise()).isNull();
        verify(exerciseUseCase, never()).getById(any(), any());
    }
}
