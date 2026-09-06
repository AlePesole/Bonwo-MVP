package com.alessandropesole.bonwoapp.routine.application.service;

import com.alessandropesole.bonwoapp.catalog.application.dto.ActivityResponse;
import com.alessandropesole.bonwoapp.catalog.application.dto.EquipmentResponse;
import com.alessandropesole.bonwoapp.catalog.application.dto.TrainingGoalResponse;
import com.alessandropesole.bonwoapp.catalog.application.mapper.ActivityDtoMapper;
import com.alessandropesole.bonwoapp.catalog.application.mapper.EquipmentDtoMapper;
import com.alessandropesole.bonwoapp.catalog.application.mapper.TrainingGoalDtoMapper;
import com.alessandropesole.bonwoapp.catalog.application.service.CatalogValidator;
import com.alessandropesole.bonwoapp.catalog.domain.model.MuscleSubGroup;
import com.alessandropesole.bonwoapp.catalog.domain.port.out.ActivityRepository;
import com.alessandropesole.bonwoapp.catalog.domain.port.out.EquipmentRepository;
import com.alessandropesole.bonwoapp.catalog.domain.port.out.MuscleSubGroupRepository;
import com.alessandropesole.bonwoapp.catalog.domain.port.out.TrainingGoalRepository;
import com.alessandropesole.bonwoapp.exercise.application.dto.ExerciseResponse;
import com.alessandropesole.bonwoapp.exercise.domain.model.Exercise;
import com.alessandropesole.bonwoapp.exercise.domain.model.MuscleSummary;
import com.alessandropesole.bonwoapp.exercise.domain.port.in.publication.ExercisePublicationUseCase;
import com.alessandropesole.bonwoapp.exercise.domain.port.in.ExerciseUseCase;
import com.alessandropesole.bonwoapp.exercise.domain.port.out.ExerciseRepository;
import com.alessandropesole.bonwoapp.exercise.application.service.publication.ExerciseVisibilityResolver;
import com.alessandropesole.bonwoapp.exercise.application.service.MuscleSummaryCalculator;
import com.alessandropesole.bonwoapp.media.application.service.MediaResolver;
import com.alessandropesole.bonwoapp.media.application.service.MediaService;
import com.alessandropesole.bonwoapp.routine.application.dto.CreateRoutineRequest;
import com.alessandropesole.bonwoapp.routine.application.dto.ExerciseSlotResponse;
import com.alessandropesole.bonwoapp.routine.application.dto.RoutineResponse;
import com.alessandropesole.bonwoapp.routine.application.dto.UpdateRoutineRequest;
import com.alessandropesole.bonwoapp.routine.application.mapper.ExerciseSlotDtoMapper;
import com.alessandropesole.bonwoapp.routine.application.mapper.RoutineDtoMapper;
import com.alessandropesole.bonwoapp.routine.domain.model.ExerciseSlot;
import com.alessandropesole.bonwoapp.routine.domain.model.Routine;
import com.alessandropesole.bonwoapp.routine.domain.model.RoutineFilter;
import com.alessandropesole.bonwoapp.routine.domain.port.in.RoutineUseCase;
import com.alessandropesole.bonwoapp.routine.domain.port.out.RoutineRepository;
import com.alessandropesole.bonwoapp.shared.infrastructure.exception.ForbiddenOperationException;
import com.alessandropesole.bonwoapp.shared.infrastructure.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RoutineService implements RoutineUseCase {

    private final RoutineRepository routineRepository;
    private final EquipmentRepository equipmentRepository;
    private final ActivityRepository activityRepository;
    private final TrainingGoalRepository trainingGoalRepository;
    private final MuscleSubGroupRepository muscleSubGroupRepository;
    private final CatalogValidator catalogValidator;
    private final MediaService mediaService;
    private final MediaResolver mediaResolver;
    private final MuscleSummaryCalculator muscleSummaryCalculator;
    private final ExerciseUseCase exerciseUseCase;
    private final ExerciseRepository exerciseRepository;
    private final ExercisePublicationUseCase exercisePublicationUseCase;
    private final ExerciseVisibilityResolver exerciseVisibilityResolver;

    @Override
    public RoutineResponse create(CreateRoutineRequest req, Long ownerId) {
        return create(req, ownerId, null, null);
    }

    @Override
    public RoutineResponse create(CreateRoutineRequest req, Long ownerId, Long trainingProgramId, Integer position) {
        catalogValidator.validate(req.equipmentIds(), req.activityIds(), req.trainingGoalIds());

        List<ExerciseSlot> slots = ExerciseSlotDtoMapper.toDomainList(req.slots());
        validateSlotExercisesAreAccessible(slots, ownerId);
        MuscleSummary muscleSummary = resolveAndAggregateMuscleSummary(slots, ownerId);

        Long thumbnailId = resolveNewThumbnailId(req.thumbnailUploadToken(), req.thumbnailId(), ownerId);

        Routine routine = Routine.create(
                ownerId, req.title(), req.description(), req.level(), thumbnailId,
                slots, req.restBetweenExercises(),
                req.equipmentIds(), req.activityIds(), req.trainingGoalIds(),
                trainingProgramId, position
        );
        routine.applyMuscleSummary(muscleSummary);

        Routine saved = routineRepository.save(routine);
        exercisePublicationUseCase.registerUses(ownerId, saved.getId(), extractExerciseIds(saved.getSlots()));
        return toResponse(saved, ownerId);
    }

    @Override
    @Transactional(readOnly = true)
    public RoutineResponse getById(Long id, Long ownerId) {
        return toResponse(findOwned(id, ownerId), ownerId);
    }

    @Override
    public RoutineResponse update(Long id, UpdateRoutineRequest req, Long ownerId) {
        return update(id, req, ownerId, null);
    }

    @Override
    public RoutineResponse update(Long id, UpdateRoutineRequest req, Long ownerId, Integer newPosition) {
        Routine routine = findOwned(id, ownerId);

        if (req.equipmentIds() != null || req.activityIds() != null || req.trainingGoalIds() != null) {
            catalogValidator.validate(
                    req.equipmentIds() != null ? req.equipmentIds() : routine.getEquipmentIds(),
                    req.activityIds() != null ? req.activityIds() : routine.getActivityIds(),
                    req.trainingGoalIds() != null ? req.trainingGoalIds() : routine.getTrainingGoalIds());
        }

        List<ExerciseSlot> newSlots = req.slots() != null
                ? ExerciseSlotDtoMapper.toDomainList(req.slots()) : null;
        if (newSlots != null) validateSlotExercisesAreAccessible(newSlots, ownerId);
        MuscleSummary newMuscleSummary = newSlots != null
                ? resolveAndAggregateMuscleSummary(newSlots, ownerId) : null;

        Long newThumbnailId = req.thumbnailUploadToken() != null
            ? mediaService.claimImage(req.thumbnailUploadToken(), ownerId) : null;

        routine.update(
                req.title(), req.description(), req.level(),
                newThumbnailId, req.removeThumbnail(),
                newSlots, req.restBetweenExercises(),
                req.equipmentIds(), req.activityIds(), req.trainingGoalIds(), newPosition
        );
        if (newMuscleSummary != null) routine.applyMuscleSummary(newMuscleSummary);

        Routine saved = routineRepository.save(routine);
        exercisePublicationUseCase.registerUses(ownerId, saved.getId(), extractExerciseIds(saved.getSlots()));

        return toResponse(saved, ownerId);
    }

    @Override
    public void delete(Long id, Long ownerId) {
        Routine routine = findOwned(id, ownerId);
        routineRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RoutineResponse> listMine(Long ownerId, RoutineFilter filter, Pageable pageable) {
        Set<Long> muscleSubGroupIds = resolveMuscleSubGroupIds(filter);
        return routineRepository.findByOwner(ownerId, muscleSubGroupIds, filter.equipmentIds(),
                        filter.activityIds(), filter.trainingGoalIds(), filter.title(), pageable)
                .map(r -> toResponse(r, ownerId));
    }

    private Set<Long> resolveMuscleSubGroupIds(RoutineFilter filter) {
        if (filter.muscleSubGroupId() != null) {
            return Set.of(filter.muscleSubGroupId());
        }
        if (filter.muscleGroupId() != null) {
            return muscleSubGroupRepository.findByGroupId(filter.muscleGroupId()).stream()
                    .map(MuscleSubGroup::getId)
                    .collect(Collectors.toSet());
        }
        return Set.of();
    }

    private Long resolveNewThumbnailId(String thumbnailUploadToken, Long thumbnailId, Long ownerId) {
        if (thumbnailUploadToken != null) return mediaService.claimImage(thumbnailUploadToken, ownerId);
        if (thumbnailId != null) {
            mediaService.verifyImageOwnership(thumbnailId, ownerId);
            return thumbnailId;
        }
        return null;
    }

    private Set<Long> extractExerciseIds(List<ExerciseSlot> slots) {
        return slots.stream().map(ExerciseSlot::getExerciseId).collect(Collectors.toSet());
    }

    private void validateSlotExercisesAreAccessible(List<ExerciseSlot> slots, Long ownerId) {
        for (ExerciseSlot slot : slots) {
            Exercise exercise = exerciseRepository.findById(slot.getExerciseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Exercise", slot.getExerciseId()));
            if (!exerciseVisibilityResolver.isVisible(exercise, ownerId))
                throw new ForbiddenOperationException(
                        "Exercise " + slot.getExerciseId() + " is not accessible to you");
        }
    }

    private MuscleSummary resolveAndAggregateMuscleSummary(List<ExerciseSlot> slots, Long ownerId) {
        List<MuscleSummary> summaries = slots.stream()
                .map(slot -> MuscleSummary.of(exerciseUseCase.getById(slot.getExerciseId(), ownerId).muscleSummary()))
                .toList();
        return muscleSummaryCalculator.aggregate(summaries);
    }

    private Routine findOwned(Long id, Long ownerId) {
        Routine routine = routineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Routine", id));
        if (!routine.isOwnedBy(ownerId))
            throw new ForbiddenOperationException("You don't own this routine");
        return routine;
    }

    private RoutineResponse toResponse(Routine r, Long ownerId) {
        List<EquipmentResponse> equipment = r.getEquipmentIds().isEmpty() ? List.of()
                : equipmentRepository.findAllById(r.getEquipmentIds()).stream()
                .map(eq -> EquipmentDtoMapper.toResponse(eq, mediaResolver.resolveImage(eq.getIconId())))
                .toList();

        List<ActivityResponse> activities = r.getActivityIds().isEmpty() ? List.of()
                : activityRepository.findAllById(r.getActivityIds()).stream()
                .map(a -> ActivityDtoMapper.toResponse(a, mediaResolver.resolveImage(a.getIconId())))
                .toList();

        List<TrainingGoalResponse> trainingGoals = r.getTrainingGoalIds().isEmpty() ? List.of()
                : trainingGoalRepository.findAllById(r.getTrainingGoalIds()).stream()
                .map(t -> TrainingGoalDtoMapper.toResponse(t, mediaResolver.resolveImage(t.getIconId())))
                .toList();

        Set<Long> exerciseIds = r.getSlots().stream()
                .map(ExerciseSlot::getExerciseId)
                .collect(Collectors.toSet());
        Map<Long, ExerciseResponse> exercises = exerciseUseCase.getVisibleByIds(exerciseIds, ownerId);

        List<ExerciseSlotResponse> slots = r.getSlots().stream()
                .map(slot -> ExerciseSlotDtoMapper.toResponse(slot, exercises.get(slot.getExerciseId())))
                .toList();

        return RoutineDtoMapper.toResponse(
                r, equipment, activities, trainingGoals,
                mediaResolver.resolveImage(r.getThumbnailId()),
                slots
        );
    }
}
