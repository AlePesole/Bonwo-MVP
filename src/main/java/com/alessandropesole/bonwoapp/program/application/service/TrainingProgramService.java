package com.alessandropesole.bonwoapp.program.application.service;

import com.alessandropesole.bonwoapp.catalog.application.dto.ActivityResponse;
import com.alessandropesole.bonwoapp.catalog.application.dto.EquipmentResponse;
import com.alessandropesole.bonwoapp.catalog.application.dto.TrainingGoalResponse;
import com.alessandropesole.bonwoapp.catalog.application.mapper.ActivityDtoMapper;
import com.alessandropesole.bonwoapp.catalog.application.mapper.EquipmentDtoMapper;
import com.alessandropesole.bonwoapp.catalog.application.mapper.TrainingGoalDtoMapper;
import com.alessandropesole.bonwoapp.catalog.application.service.CatalogValidator;
import com.alessandropesole.bonwoapp.catalog.domain.port.out.ActivityRepository;
import com.alessandropesole.bonwoapp.catalog.domain.port.out.EquipmentRepository;
import com.alessandropesole.bonwoapp.catalog.domain.port.out.TrainingGoalRepository;
import com.alessandropesole.bonwoapp.exercise.application.service.MuscleSummaryCalculator;
import com.alessandropesole.bonwoapp.exercise.domain.model.MuscleSummary;
import com.alessandropesole.bonwoapp.media.application.service.MediaResolver;
import com.alessandropesole.bonwoapp.media.application.service.MediaService;
import com.alessandropesole.bonwoapp.program.application.dto.CreateTrainingProgramRequest;
import com.alessandropesole.bonwoapp.program.application.dto.ProgramRoutineDto;
import com.alessandropesole.bonwoapp.program.application.dto.TrainingProgramResponse;
import com.alessandropesole.bonwoapp.program.application.dto.UpdateTrainingProgramRequest;
import com.alessandropesole.bonwoapp.program.application.mapper.TrainingProgramDtoMapper;
import com.alessandropesole.bonwoapp.program.domain.exception.InvalidTrainingProgramException;
import com.alessandropesole.bonwoapp.program.domain.model.TrainingProgram;
import com.alessandropesole.bonwoapp.program.domain.model.TrainingProgramFilter;
import com.alessandropesole.bonwoapp.program.domain.port.in.TrainingProgramUseCase;
import com.alessandropesole.bonwoapp.program.domain.port.out.TrainingProgramRepository;
import com.alessandropesole.bonwoapp.routine.application.dto.CreateRoutineRequest;
import com.alessandropesole.bonwoapp.routine.application.dto.RoutineResponse;
import com.alessandropesole.bonwoapp.routine.application.dto.UpdateRoutineRequest;
import com.alessandropesole.bonwoapp.routine.domain.model.Routine;
import com.alessandropesole.bonwoapp.routine.domain.port.in.RoutineUseCase;
import com.alessandropesole.bonwoapp.routine.domain.port.out.RoutineRepository;
import com.alessandropesole.bonwoapp.shared.infrastructure.exception.ForbiddenOperationException;
import com.alessandropesole.bonwoapp.shared.infrastructure.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class TrainingProgramService implements TrainingProgramUseCase {

    private final TrainingProgramRepository trainingProgramRepository;
    private final EquipmentRepository equipmentRepository;
    private final ActivityRepository activityRepository;
    private final TrainingGoalRepository trainingGoalRepository;
    private final CatalogValidator catalogValidator;
    private final MediaService mediaService;
    private final MediaResolver mediaResolver;
    private final MuscleSummaryCalculator muscleSummaryCalculator;
    private final RoutineUseCase routineUseCase;
    private final RoutineRepository routineRepository;

    @Override
    public TrainingProgramResponse create(CreateTrainingProgramRequest req, Long ownerId) {
        catalogValidator.validate(req.equipmentIds(), req.activityIds(), req.trainingGoalIds());
        validateNoDuplicatePositions(req.routines());

        Long thumbnailId = req.thumbnailUploadToken() != null
                ? mediaService.claimImage(req.thumbnailUploadToken(), ownerId) : null;

        TrainingProgram program = TrainingProgram.create(
                ownerId, req.title(), req.description(), req.level(), thumbnailId,
                req.daysPerWeek(), req.equipmentIds(), req.activityIds(), req.trainingGoalIds()
        );
        TrainingProgram saved = trainingProgramRepository.save(program);
        Long programId = saved.getId();

        List<RoutineResponse> routines = req.routines() == null ? List.of()
                : req.routines().stream().map(dto -> createProgramRoutine(dto, programId, ownerId)).toList();

        saved.applyMuscleSummary(aggregateMuscleSummary(routines));
        saved = trainingProgramRepository.save(saved);

        return toResponse(saved, routines, ownerId);
    }

    @Override
    @Transactional(readOnly = true)
    public TrainingProgramResponse getById(Long id, Long ownerId) {
        TrainingProgram program = findOwned(id, ownerId);
        return toResponse(program, fetchRoutines(id, ownerId), ownerId);
    }

    @Override
    public TrainingProgramResponse update(Long id, UpdateTrainingProgramRequest req, Long ownerId) {
        TrainingProgram program = findOwned(id, ownerId);

        if (req.equipmentIds() != null || req.activityIds() != null || req.trainingGoalIds() != null) {
            catalogValidator.validate(
                    req.equipmentIds() != null ? req.equipmentIds() : program.getEquipmentIds(),
                    req.activityIds() != null ? req.activityIds() : program.getActivityIds(),
                    req.trainingGoalIds() != null ? req.trainingGoalIds() : program.getTrainingGoalIds());
        }

        Long oldThumbnailId = program.getThumbnailId();
        Long newThumbnailId = req.thumbnailUploadToken() != null
                ? mediaService.claimImage(req.thumbnailUploadToken(), ownerId) : null;

        program.update(
                req.title(), req.description(), req.level(),
                newThumbnailId, req.removeThumbnail(),
                req.daysPerWeek(),
                req.equipmentIds(), req.activityIds(), req.trainingGoalIds()
        );
        TrainingProgram saved = trainingProgramRepository.save(program);

        if (newThumbnailId != null || req.removeThumbnail())
            mediaService.deleteImageIfOwner(oldThumbnailId, ownerId);

        List<RoutineResponse> routines;
        if (req.routines() != null) {
            validateNoDuplicatePositions(req.routines());
            routines = diffAndApplyRoutines(req.routines(), id, ownerId);
            saved.applyMuscleSummary(aggregateMuscleSummary(routines));
            saved = trainingProgramRepository.save(saved);
        } else {
            routines = fetchRoutines(id, ownerId);
        }

        return toResponse(saved, routines, ownerId);
    }

    @Override
    public void delete(Long id, Long ownerId) {
        TrainingProgram program = findOwned(id, ownerId);
        mediaService.deleteImageIfOwner(program.getThumbnailId(), ownerId);
        routineRepository.findByTrainingProgramId(id).forEach(r -> routineUseCase.delete(r.getId(), ownerId));
        trainingProgramRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TrainingProgramResponse> listMine(Long ownerId, TrainingProgramFilter filter, Pageable pageable) {
        return trainingProgramRepository.findByOwner(ownerId, filter.equipmentIds(), filter.activityIds(),
                        filter.trainingGoalIds(), pageable)
                .map(p -> toResponse(p, fetchRoutines(p.getId(), ownerId), ownerId));
    }

    private RoutineResponse createProgramRoutine(ProgramRoutineDto dto, Long trainingProgramId, Long ownerId) {
        CreateRoutineRequest routineReq = new CreateRoutineRequest(
                dto.title(), dto.description(), dto.level(), dto.thumbnailUploadToken(),
                dto.slots(), dto.restBetweenExercises(),
                dto.equipmentIds(), dto.activityIds(), dto.trainingGoalIds());
        return routineUseCase.create(routineReq, ownerId, trainingProgramId, dto.position());
    }

    /**
     * Full-list-replace, same convention as Routine's own slots: any existing program routine whose id
     * isn't present in dtos gets deleted; entries with an id get updated in place (content + position);
     * entries without one are created fresh. Reuses RoutineUseCase entirely — TrainingProgramService
     * never touches slot/thumbnail logic directly, avoiding any duplication of what Routine already does.
     */
    private List<RoutineResponse> diffAndApplyRoutines(List<ProgramRoutineDto> dtos, Long programId, Long ownerId) {
        List<Routine> existing = routineRepository.findByTrainingProgramId(programId);
        Set<Long> incomingIds = new HashSet<>();
        for (ProgramRoutineDto dto : dtos) if (dto.id() != null) incomingIds.add(dto.id());

        for (Routine r : existing) {
            if (!incomingIds.contains(r.getId())) {
                routineUseCase.delete(r.getId(), ownerId);
            }
        }

        return dtos.stream().map(dto -> {
            if (dto.id() == null) return createProgramRoutine(dto, programId, ownerId);

            boolean belongsToThisProgram = existing.stream().anyMatch(r -> r.getId().equals(dto.id()));
            if (!belongsToThisProgram)
                throw new InvalidTrainingProgramException(
                        "Routine " + dto.id() + " doesn't belong to this training program");

            UpdateRoutineRequest updateReq = new UpdateRoutineRequest(
                    dto.title(), dto.description(), dto.level(), dto.thumbnailUploadToken(),
                    dto.removeThumbnail(), dto.slots(), dto.restBetweenExercises(),
                    dto.equipmentIds(), dto.activityIds(), dto.trainingGoalIds());
            return routineUseCase.update(dto.id(), updateReq, ownerId, dto.position());
        }).toList();
    }

    private List<RoutineResponse> fetchRoutines(Long trainingProgramId, Long ownerId) {
        return routineRepository.findByTrainingProgramId(trainingProgramId).stream()
                .sorted(Comparator.comparing(r -> r.getPosition() != null ? r.getPosition() : 0))
                .map(r -> routineUseCase.getById(r.getId(), ownerId))
                .toList();
    }

    private MuscleSummary aggregateMuscleSummary(List<RoutineResponse> routines) {
        List<MuscleSummary> summaries = routines.stream().map(r -> MuscleSummary.of(r.muscleSummary())).toList();
        return muscleSummaryCalculator.aggregate(summaries);
    }

    private void validateNoDuplicatePositions(List<ProgramRoutineDto> routines) {
        if (routines == null || routines.isEmpty()) return;
        long distinctCount = routines.stream().map(ProgramRoutineDto::position).distinct().count();
        if (distinctCount != routines.size())
            throw new InvalidTrainingProgramException("Two routines cannot share the same position");
    }

    private TrainingProgram findOwned(Long id, Long ownerId) {
        TrainingProgram program = trainingProgramRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TrainingProgram", id));
        if (!program.isOwnedBy(ownerId))
            throw new ForbiddenOperationException("You don't own this training program");
        return program;
    }

    private TrainingProgramResponse toResponse(TrainingProgram p, List<RoutineResponse> routines, Long ownerId) {
        List<EquipmentResponse> equipment = p.getEquipmentIds().isEmpty() ? List.of()
                : equipmentRepository.findAllById(p.getEquipmentIds()).stream()
                .map(eq -> EquipmentDtoMapper.toResponse(eq, mediaResolver.resolveImage(eq.getIconId())))
                .toList();

        List<ActivityResponse> activities = p.getActivityIds().isEmpty() ? List.of()
                : activityRepository.findAllById(p.getActivityIds()).stream()
                .map(a -> ActivityDtoMapper.toResponse(a, mediaResolver.resolveImage(a.getIconId())))
                .toList();

        List<TrainingGoalResponse> trainingGoals = p.getTrainingGoalIds().isEmpty() ? List.of()
                : trainingGoalRepository.findAllById(p.getTrainingGoalIds()).stream()
                .map(t -> TrainingGoalDtoMapper.toResponse(t, mediaResolver.resolveImage(t.getIconId())))
                .toList();

        return TrainingProgramDtoMapper.toResponse(
                p, equipment, activities, trainingGoals,
                mediaResolver.resolveImage(p.getThumbnailId()), routines
        );
    }
}
