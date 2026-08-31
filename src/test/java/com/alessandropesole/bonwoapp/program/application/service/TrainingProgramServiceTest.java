package com.alessandropesole.bonwoapp.program.application.service;

import com.alessandropesole.bonwoapp.catalog.application.service.CatalogValidator;
import com.alessandropesole.bonwoapp.catalog.domain.port.out.ActivityRepository;
import com.alessandropesole.bonwoapp.catalog.domain.port.out.EquipmentRepository;
import com.alessandropesole.bonwoapp.catalog.domain.port.out.TrainingGoalRepository;
import com.alessandropesole.bonwoapp.exercise.application.service.MuscleSummaryCalculator;
import com.alessandropesole.bonwoapp.exercise.domain.model.Level;
import com.alessandropesole.bonwoapp.exercise.domain.model.MuscleSummary;
import com.alessandropesole.bonwoapp.media.application.service.MediaResolver;
import com.alessandropesole.bonwoapp.media.application.service.MediaService;
import com.alessandropesole.bonwoapp.program.application.dto.CreateTrainingProgramRequest;
import com.alessandropesole.bonwoapp.program.application.dto.ProgramRoutineDto;
import com.alessandropesole.bonwoapp.program.application.dto.TrainingProgramResponse;
import com.alessandropesole.bonwoapp.program.application.dto.UpdateTrainingProgramRequest;
import com.alessandropesole.bonwoapp.program.domain.exception.InvalidTrainingProgramException;
import com.alessandropesole.bonwoapp.program.domain.model.TrainingProgram;
import com.alessandropesole.bonwoapp.program.domain.model.TrainingProgramFilter;
import com.alessandropesole.bonwoapp.program.domain.port.out.TrainingProgramRepository;
import com.alessandropesole.bonwoapp.routine.application.dto.CreateRoutineRequest;
import com.alessandropesole.bonwoapp.routine.application.dto.RoutineResponse;
import com.alessandropesole.bonwoapp.routine.application.dto.UpdateRoutineRequest;
import com.alessandropesole.bonwoapp.routine.domain.model.Routine;
import com.alessandropesole.bonwoapp.routine.domain.port.in.RoutineUseCase;
import com.alessandropesole.bonwoapp.routine.domain.port.out.RoutineRepository;
import com.alessandropesole.bonwoapp.shared.infrastructure.exception.ForbiddenOperationException;
import com.alessandropesole.bonwoapp.shared.infrastructure.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrainingProgramServiceTest {

    @Mock private TrainingProgramRepository trainingProgramRepository;
    @Mock private EquipmentRepository equipmentRepository;
    @Mock private ActivityRepository activityRepository;
    @Mock private TrainingGoalRepository trainingGoalRepository;
    @Mock private CatalogValidator catalogValidator;
    @Mock private MediaService mediaService;
    @Mock private MediaResolver mediaResolver;
    @Mock private MuscleSummaryCalculator muscleSummaryCalculator;
    @Mock private RoutineUseCase routineUseCase;
    @Mock private RoutineRepository routineRepository;

    @InjectMocks
    private TrainingProgramService trainingProgramService;

    private static final Long OWNER_ID = 1L;
    private static final Long PROGRAM_ID = 10L;

    private static ProgramRoutineDto routineDto(Long id, int position) {
        return new ProgramRoutineDto(id, "Push Day", null, Level.INTERMEDIATE, null, null, false,
                position, List.of(), null, Set.of(), Set.of(), Set.of());
    }

    private static RoutineResponse routineResponse(Long id, int position) {
        return new RoutineResponse(id, OWNER_ID, "Push Day", null, Level.INTERMEDIATE,
                null, null, null, List.of(), Map.of(1L, 0.5),
                List.of(), List.of(), List.of(), null, PROGRAM_ID, position);
    }

    private static Routine existingRoutine(Long id, int position) {
        return Routine.reconstitute(id, OWNER_ID, "Push Day", null, Level.INTERMEDIATE,
                null, java.time.Duration.ZERO, List.of(), null, MuscleSummary.empty(),
                Set.of(), Set.of(), Set.of(), null, PROGRAM_ID, position);
    }

    @Test
    void create_savesProgramAndCreatesEachRoutineAttachedToIt() {
        CreateTrainingProgramRequest req = new CreateTrainingProgramRequest(
                "8 Week Program", null, Level.INTERMEDIATE, null, null, 3,
                List.of(routineDto(null, 1)), Set.of(), Set.of(), Set.of());
        when(trainingProgramRepository.save(any(TrainingProgram.class))).thenAnswer(inv -> {
            TrainingProgram p = inv.getArgument(0);
            return TrainingProgram.reconstitute(PROGRAM_ID, p.getOwnerId(), p.getTitle(), p.getDescription(),
                    p.getLevel(), p.getThumbnailId(), p.getDaysPerWeek(), p.getMuscleSummary(),
                    p.getEquipmentIds(), p.getActivityIds(), p.getTrainingGoalIds(), null);
        });
        when(routineUseCase.create(any(CreateRoutineRequest.class), eq(OWNER_ID), eq(PROGRAM_ID), eq(1)))
                .thenReturn(routineResponse(100L, 1));
        when(muscleSummaryCalculator.aggregate(anyList())).thenReturn(MuscleSummary.of(Map.of(1L, 0.5)));

        TrainingProgramResponse response = trainingProgramService.create(req, OWNER_ID);

        assertThat(response.routines()).hasSize(1);
        assertThat(response.routines().get(0).id()).isEqualTo(100L);
        verify(catalogValidator).validate(Set.of(), Set.of(), Set.of());
        verify(routineUseCase).create(any(CreateRoutineRequest.class), eq(OWNER_ID), eq(PROGRAM_ID), eq(1));
    }

    @Test
    void create_rejectsDuplicatePositionsAmongRoutines() {
        CreateTrainingProgramRequest req = new CreateTrainingProgramRequest(
                "Program", null, Level.INTERMEDIATE, null, null, 3,
                List.of(routineDto(null, 1), routineDto(null, 1)), Set.of(), Set.of(), Set.of());

        assertThatThrownBy(() -> trainingProgramService.create(req, OWNER_ID))
                .isInstanceOf(InvalidTrainingProgramException.class);
        verifyNoInteractions(trainingProgramRepository);
    }

    @Test
    void create_claimsThumbnailWhenTokenProvided() {
        CreateTrainingProgramRequest req = new CreateTrainingProgramRequest(
                "Program", null, Level.INTERMEDIATE, "thumb-token", null, 3,
                List.of(), Set.of(), Set.of(), Set.of());
        when(mediaService.claimImage("thumb-token", OWNER_ID)).thenReturn(20L);
        when(trainingProgramRepository.save(any(TrainingProgram.class))).thenAnswer(inv -> inv.getArgument(0));
        when(muscleSummaryCalculator.aggregate(anyList())).thenReturn(MuscleSummary.empty());

        trainingProgramService.create(req, OWNER_ID);

        verify(mediaService).claimImage("thumb-token", OWNER_ID);
    }

    @Test
    void create_reusesExistingOwnedThumbnailIdWithoutClaimingAnUpload() {
        CreateTrainingProgramRequest req = new CreateTrainingProgramRequest(
                "Program Copy", null, Level.INTERMEDIATE, null, 77L, 3,
                List.of(), Set.of(), Set.of(), Set.of());
        when(trainingProgramRepository.save(any(TrainingProgram.class))).thenAnswer(inv -> inv.getArgument(0));
        when(muscleSummaryCalculator.aggregate(anyList())).thenReturn(MuscleSummary.empty());

        trainingProgramService.create(req, OWNER_ID);

        verify(mediaService).verifyImageOwnership(77L, OWNER_ID);
        verify(mediaService, never()).claimImage(any(), any());
    }

    @Test
    void create_prefersUploadTokenOverThumbnailIdWhenBothProvided() {
        CreateTrainingProgramRequest req = new CreateTrainingProgramRequest(
                "Program Copy", null, Level.INTERMEDIATE, "fresh-token", 77L, 3,
                List.of(), Set.of(), Set.of(), Set.of());
        when(mediaService.claimImage("fresh-token", OWNER_ID)).thenReturn(200L);
        when(trainingProgramRepository.save(any(TrainingProgram.class))).thenAnswer(inv -> inv.getArgument(0));
        when(muscleSummaryCalculator.aggregate(anyList())).thenReturn(MuscleSummary.empty());

        trainingProgramService.create(req, OWNER_ID);

        verify(mediaService).claimImage("fresh-token", OWNER_ID);
        verify(mediaService, never()).verifyImageOwnership(any(), any());
    }

    @Test
    void getById_throwsNotFoundWhenMissing() {
        when(trainingProgramRepository.findById(PROGRAM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trainingProgramService.getById(PROGRAM_ID, OWNER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getById_throwsForbiddenWhenNotOwner() {
        TrainingProgram program = TrainingProgram.reconstitute(PROGRAM_ID, 999L, "Program", null,
                Level.INTERMEDIATE, null, 3, MuscleSummary.empty(), Set.of(), Set.of(), Set.of(), null);
        when(trainingProgramRepository.findById(PROGRAM_ID)).thenReturn(Optional.of(program));

        assertThatThrownBy(() -> trainingProgramService.getById(PROGRAM_ID, OWNER_ID))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void update_removesThumbnailWithoutPhysicallyDeletingTheImage() {
        TrainingProgram program = TrainingProgram.reconstitute(PROGRAM_ID, OWNER_ID, "Program", null,
                Level.INTERMEDIATE, 50L, 3, MuscleSummary.empty(), Set.of(), Set.of(), Set.of(), null);
        when(trainingProgramRepository.findById(PROGRAM_ID)).thenReturn(Optional.of(program));
        when(trainingProgramRepository.save(any(TrainingProgram.class))).thenAnswer(inv -> inv.getArgument(0));
        when(routineRepository.findByTrainingProgramId(PROGRAM_ID)).thenReturn(List.of());
        UpdateTrainingProgramRequest req = new UpdateTrainingProgramRequest(
                null, null, null, null, true, null, null, null, null, null);

        TrainingProgramResponse response = trainingProgramService.update(PROGRAM_ID, req, OWNER_ID);

        assertThat(response.thumbnail()).isNull();
        verify(mediaService, never()).deleteImageIfOwner(any(), any());
    }

    @Test
    void update_diffsRoutines_updatesExisting_createsNew_deletesRemoved() {
        TrainingProgram program = TrainingProgram.reconstitute(PROGRAM_ID, OWNER_ID, "Program", null,
                Level.INTERMEDIATE, null, 3, MuscleSummary.empty(), Set.of(), Set.of(), Set.of(), null);
        when(trainingProgramRepository.findById(PROGRAM_ID)).thenReturn(Optional.of(program));
        when(trainingProgramRepository.save(any(TrainingProgram.class))).thenAnswer(inv -> inv.getArgument(0));
        when(routineRepository.findByTrainingProgramId(PROGRAM_ID))
                .thenReturn(List.of(existingRoutine(1L, 1), existingRoutine(2L, 2)));
        when(routineUseCase.update(eq(1L), any(UpdateRoutineRequest.class), eq(OWNER_ID), eq(2)))
                .thenReturn(routineResponse(1L, 2));
        when(routineUseCase.create(any(CreateRoutineRequest.class), eq(OWNER_ID), eq(PROGRAM_ID), eq(1)))
                .thenReturn(routineResponse(3L, 1));
        when(muscleSummaryCalculator.aggregate(anyList())).thenReturn(MuscleSummary.empty());

        UpdateTrainingProgramRequest req = new UpdateTrainingProgramRequest(
                null, null, null, null, false, null,
                List.of(routineDto(1L, 2), routineDto(null, 1)), null, null, null);

        TrainingProgramResponse response = trainingProgramService.update(PROGRAM_ID, req, OWNER_ID);

        assertThat(response.routines()).hasSize(2);
        verify(routineUseCase).update(eq(1L), any(UpdateRoutineRequest.class), eq(OWNER_ID), eq(2));
        verify(routineUseCase).create(any(CreateRoutineRequest.class), eq(OWNER_ID), eq(PROGRAM_ID), eq(1));
        verify(routineUseCase).delete(2L, OWNER_ID);
        verify(routineUseCase, never()).delete(1L, OWNER_ID);
    }

    @Test
    void update_rejectsRoutineIdNotBelongingToThisProgram() {
        TrainingProgram program = TrainingProgram.reconstitute(PROGRAM_ID, OWNER_ID, "Program", null,
                Level.INTERMEDIATE, null, 3, MuscleSummary.empty(), Set.of(), Set.of(), Set.of(), null);
        when(trainingProgramRepository.findById(PROGRAM_ID)).thenReturn(Optional.of(program));
        when(trainingProgramRepository.save(any(TrainingProgram.class))).thenAnswer(inv -> inv.getArgument(0));
        when(routineRepository.findByTrainingProgramId(PROGRAM_ID)).thenReturn(List.of(existingRoutine(1L, 1)));

        UpdateTrainingProgramRequest req = new UpdateTrainingProgramRequest(
                null, null, null, null, false, null,
                List.of(routineDto(99L, 1)), null, null, null);

        assertThatThrownBy(() -> trainingProgramService.update(PROGRAM_ID, req, OWNER_ID))
                .isInstanceOf(InvalidTrainingProgramException.class);
    }

    @Test
    void delete_deletesEveryOwnedRoutineThenTheProgramWithoutTouchingThumbnailImage() {
        TrainingProgram program = TrainingProgram.reconstitute(PROGRAM_ID, OWNER_ID, "Program", null,
                Level.INTERMEDIATE, 50L, 3, MuscleSummary.empty(), Set.of(), Set.of(), Set.of(), null);
        when(trainingProgramRepository.findById(PROGRAM_ID)).thenReturn(Optional.of(program));
        when(routineRepository.findByTrainingProgramId(PROGRAM_ID))
                .thenReturn(List.of(existingRoutine(1L, 1), existingRoutine(2L, 2)));

        trainingProgramService.delete(PROGRAM_ID, OWNER_ID);

        verify(mediaService, never()).deleteImageIfOwner(any(), any());
        verify(routineUseCase).delete(1L, OWNER_ID);
        verify(routineUseCase).delete(2L, OWNER_ID);
        verify(trainingProgramRepository).deleteById(PROGRAM_ID);
    }
}
