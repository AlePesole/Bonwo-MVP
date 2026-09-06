package com.alessandropesole.bonwoapp.session.application.service;

import com.alessandropesole.bonwoapp.exercise.application.dto.ExerciseResponse;
import com.alessandropesole.bonwoapp.exercise.application.service.MuscleSummaryCalculator;
import com.alessandropesole.bonwoapp.exercise.application.service.publication.ExerciseVisibilityResolver;
import com.alessandropesole.bonwoapp.exercise.domain.model.Exercise;
import com.alessandropesole.bonwoapp.exercise.domain.model.Level;
import com.alessandropesole.bonwoapp.exercise.domain.model.MuscleSummary;
import com.alessandropesole.bonwoapp.exercise.domain.port.in.ExerciseUseCase;
import com.alessandropesole.bonwoapp.exercise.domain.port.out.ExerciseRepository;
import com.alessandropesole.bonwoapp.routine.domain.model.ExerciseSlot;
import com.alessandropesole.bonwoapp.routine.domain.model.Routine;
import com.alessandropesole.bonwoapp.routine.domain.model.SetConfig;
import com.alessandropesole.bonwoapp.routine.domain.port.out.RoutineRepository;
import com.alessandropesole.bonwoapp.session.application.dto.CompleteTrainingSessionRequest;
import com.alessandropesole.bonwoapp.session.application.dto.StartTrainingSessionRequest;
import com.alessandropesole.bonwoapp.session.application.dto.TrainingSessionResponse;
import com.alessandropesole.bonwoapp.session.application.dto.TrainingSetDto;
import com.alessandropesole.bonwoapp.session.application.dto.TrainingSlotDto;
import com.alessandropesole.bonwoapp.session.application.dto.UpdateTrainingSessionRequest;
import com.alessandropesole.bonwoapp.session.domain.exception.SessionAlreadyCompletedException;
import com.alessandropesole.bonwoapp.session.domain.exception.SessionAlreadyInProgressException;
import com.alessandropesole.bonwoapp.session.domain.model.SessionStatus;
import com.alessandropesole.bonwoapp.session.domain.model.TrainingSession;
import com.alessandropesole.bonwoapp.session.domain.model.TrainingSlot;
import com.alessandropesole.bonwoapp.session.domain.port.out.TrainingSessionRepository;
import com.alessandropesole.bonwoapp.routine.domain.model.SetType;
import com.alessandropesole.bonwoapp.shared.infrastructure.exception.ForbiddenOperationException;
import com.alessandropesole.bonwoapp.shared.infrastructure.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrainingSessionServiceTest {

    @Mock private TrainingSessionRepository trainingSessionRepository;
    @Mock private RoutineRepository routineRepository;
    @Mock private ExerciseRepository exerciseRepository;
    @Mock private ExerciseUseCase exerciseUseCase;
    @Mock private ExerciseVisibilityResolver exerciseVisibilityResolver;
    @Mock private MuscleSummaryCalculator muscleSummaryCalculator;

    @InjectMocks
    private TrainingSessionService trainingSessionService;

    private static final Long OWNER_ID = 1L;
    private static final Long ROUTINE_ID = 10L;
    private static final Long EXERCISE_ID = 5L;
    private static final Long SESSION_ID = 100L;

    private static Exercise ownedExercise() {
        return Exercise.reconstitute(EXERCISE_ID, OWNER_ID, "Bench Press", Level.INTERMEDIATE,
                null, null, null, null, List.of(), null, Set.of(), Set.of(), Set.of(), null, null);
    }

    private static ExerciseResponse exerciseResponse(Map<Long, Double> muscleSummary) {
        return new ExerciseResponse(EXERCISE_ID, OWNER_ID, "Bench Press", Level.INTERMEDIATE,
                null, null, null, null, muscleSummary, List.of(), List.of(), List.of(), List.of(), null, null);
    }

    private static Routine ownedRoutine() {
        ExerciseSlot slot = ExerciseSlot.create(EXERCISE_ID, 1, List.of(SetConfig.reps(10, null, null)), null);
        return Routine.reconstitute(ROUTINE_ID, OWNER_ID, "Push Day", null, Level.INTERMEDIATE,
                null, Duration.ZERO, List.of(slot), null, MuscleSummary.empty(),
                Set.of(), Set.of(), Set.of(), null, null, null);
    }

    private static TrainingSlot ownedSlot(boolean done) {
        return TrainingSlot.create(EXERCISE_ID, 1,
                List.of(com.alessandropesole.bonwoapp.session.domain.model.TrainingSet.reps(10, null, null, done)), null);
    }

    private static TrainingSession inProgressSession() {
        return TrainingSession.start(OWNER_ID, ROUTINE_ID, "Push Day", null, List.of(ownedSlot(false)));
    }

    @Test
    void start_clonesRoutineSlotsAndAggregatesMuscleSummary() {
        when(routineRepository.findById(ROUTINE_ID)).thenReturn(Optional.of(ownedRoutine()));
        Exercise exercise = ownedExercise();
        when(exerciseRepository.findById(EXERCISE_ID)).thenReturn(Optional.of(exercise));
        when(exerciseVisibilityResolver.isVisible(exercise, OWNER_ID)).thenReturn(true);
        when(exerciseUseCase.getById(EXERCISE_ID, OWNER_ID)).thenReturn(exerciseResponse(Map.of(1L, 0.8)));
        when(muscleSummaryCalculator.aggregate(anyList())).thenReturn(MuscleSummary.of(Map.of(1L, 0.8)));
        when(trainingSessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TrainingSessionResponse response = trainingSessionService.start(
                new StartTrainingSessionRequest(ROUTINE_ID), OWNER_ID);

        assertThat(response.status()).isEqualTo(SessionStatus.IN_PROGRESS);
        assertThat(response.routineId()).isEqualTo(ROUTINE_ID);
        assertThat(response.routineTitle()).isEqualTo("Push Day");
        assertThat(response.slots()).hasSize(1);
        assertThat(response.slots().get(0).sets().get(0).done()).isFalse();
        assertThat(response.muscleSummary()).isEqualTo(Map.of(1L, 0.8));
    }

    @Test
    void start_throwsForbiddenWhenRoutineNotOwnedByCaller() {
        Routine othersRoutine = Routine.reconstitute(ROUTINE_ID, 999L, "Push Day", null, Level.INTERMEDIATE,
                null, Duration.ZERO, List.of(), null, MuscleSummary.empty(),
                Set.of(), Set.of(), Set.of(), null, null, null);
        when(routineRepository.findById(ROUTINE_ID)).thenReturn(Optional.of(othersRoutine));

        assertThatThrownBy(() -> trainingSessionService.start(new StartTrainingSessionRequest(ROUTINE_ID), OWNER_ID))
                .isInstanceOf(ForbiddenOperationException.class);
        verify(trainingSessionRepository, never()).save(any());
    }

    @Test
    void start_throwsNotFoundWhenRoutineDoesNotExist() {
        when(routineRepository.findById(ROUTINE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trainingSessionService.start(new StartTrainingSessionRequest(ROUTINE_ID), OWNER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(trainingSessionRepository, never()).save(any());
    }

    @Test
    void start_throwsWhenCallerAlreadyHasASessionInProgress() {
        when(trainingSessionRepository.existsByOwnerAndStatus(OWNER_ID, SessionStatus.IN_PROGRESS)).thenReturn(true);

        assertThatThrownBy(() -> trainingSessionService.start(new StartTrainingSessionRequest(ROUTINE_ID), OWNER_ID))
                .isInstanceOf(SessionAlreadyInProgressException.class);
        verify(routineRepository, never()).findById(any());
        verify(trainingSessionRepository, never()).save(any());
    }

    @Test
    void update_replacesSlotsAndRecomputesMuscleSummary() {
        TrainingSession session = inProgressSession();
        when(trainingSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
        Exercise exercise = ownedExercise();
        when(exerciseRepository.findById(EXERCISE_ID)).thenReturn(Optional.of(exercise));
        when(exerciseVisibilityResolver.isVisible(exercise, OWNER_ID)).thenReturn(true);
        when(exerciseUseCase.getById(EXERCISE_ID, OWNER_ID)).thenReturn(exerciseResponse(Map.of(1L, 0.5)));
        when(muscleSummaryCalculator.aggregate(anyList())).thenReturn(MuscleSummary.of(Map.of(1L, 0.5)));
        when(trainingSessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var req = new UpdateTrainingSessionRequest(
                List.of(new TrainingSlotDto(EXERCISE_ID, 1,
                        List.of(new TrainingSetDto(SetType.REPS, 10, null, null, null, true)), null)),
                "halfway note");

        TrainingSessionResponse response = trainingSessionService.update(SESSION_ID, req, OWNER_ID);

        assertThat(response.slots().get(0).sets().get(0).done()).isTrue();
        assertThat(response.finalNote()).isEqualTo("halfway note");
        assertThat(response.muscleSummary()).isEqualTo(Map.of(1L, 0.5));
    }

    @Test
    void update_throwsForbiddenWhenSessionNotOwnedByCaller() {
        TrainingSession othersSession = TrainingSession.start(999L, ROUTINE_ID, "Push Day", null, List.of(ownedSlot(false)));
        when(trainingSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(othersSession));

        assertThatThrownBy(() -> trainingSessionService.update(SESSION_ID,
                new UpdateTrainingSessionRequest(null, "note"), OWNER_ID))
                .isInstanceOf(ForbiddenOperationException.class);
        verify(trainingSessionRepository, never()).save(any());
    }

    @Test
    void complete_setsCompletedStatusAndFinalNote() {
        TrainingSession session = inProgressSession();
        when(trainingSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
        when(trainingSessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Exercise exercise = ownedExercise();
        when(exerciseRepository.findById(EXERCISE_ID)).thenReturn(Optional.of(exercise));
        when(exerciseVisibilityResolver.isVisible(exercise, OWNER_ID)).thenReturn(true);
        when(exerciseUseCase.getById(EXERCISE_ID, OWNER_ID)).thenReturn(exerciseResponse(Map.of()));

        TrainingSessionResponse response = trainingSessionService.complete(
                SESSION_ID, new CompleteTrainingSessionRequest("great session"), OWNER_ID);

        assertThat(response.status()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(response.completedAt()).isNotNull();
        assertThat(response.duration()).isNotNull();
        assertThat(response.finalNote()).isEqualTo("great session");
    }

    @Test
    void complete_throwsWhenAlreadyCompleted() {
        TrainingSession session = inProgressSession();
        session.complete(null);
        when(trainingSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> trainingSessionService.complete(
                SESSION_ID, new CompleteTrainingSessionRequest(null), OWNER_ID))
                .isInstanceOf(SessionAlreadyCompletedException.class);
        verify(trainingSessionRepository, never()).save(any());
    }

    @Test
    void delete_verifiesOwnershipBeforeDeleting() {
        when(trainingSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(inProgressSession()));

        trainingSessionService.delete(SESSION_ID, OWNER_ID);

        verify(trainingSessionRepository).deleteById(SESSION_ID);
    }

    @Test
    void delete_throwsForbiddenWhenNotOwner() {
        TrainingSession othersSession = TrainingSession.start(999L, ROUTINE_ID, "Push Day", null, List.of(ownedSlot(false)));
        when(trainingSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(othersSession));

        assertThatThrownBy(() -> trainingSessionService.delete(SESSION_ID, OWNER_ID))
                .isInstanceOf(ForbiddenOperationException.class);
        verify(trainingSessionRepository, never()).deleteById(any());
    }
}
