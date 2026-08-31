package com.alessandropesole.bonwoapp.exercise.application.service;

import com.alessandropesole.bonwoapp.catalog.application.dto.ActivityResponse;
import com.alessandropesole.bonwoapp.catalog.application.dto.EquipmentResponse;
import com.alessandropesole.bonwoapp.catalog.application.dto.TrainingGoalResponse;
import com.alessandropesole.bonwoapp.catalog.application.mapper.ActivityDtoMapper;
import com.alessandropesole.bonwoapp.catalog.application.mapper.EquipmentDtoMapper;
import com.alessandropesole.bonwoapp.catalog.application.mapper.MuscleSubGroupDtoMapper;
import com.alessandropesole.bonwoapp.catalog.application.mapper.TrainingGoalDtoMapper;
import com.alessandropesole.bonwoapp.catalog.application.service.CatalogValidator;
import com.alessandropesole.bonwoapp.catalog.domain.model.MuscleSubGroup;
import com.alessandropesole.bonwoapp.catalog.domain.port.out.ActivityRepository;
import com.alessandropesole.bonwoapp.catalog.domain.port.out.EquipmentRepository;
import com.alessandropesole.bonwoapp.catalog.domain.port.out.MuscleSubGroupRepository;
import com.alessandropesole.bonwoapp.catalog.domain.port.out.TrainingGoalRepository;
import com.alessandropesole.bonwoapp.exercise.application.dto.CreateExerciseRequest;
import com.alessandropesole.bonwoapp.exercise.application.dto.ExerciseResponse;
import com.alessandropesole.bonwoapp.exercise.application.dto.MuscleEntryResponse;
import com.alessandropesole.bonwoapp.exercise.application.dto.UpdateExerciseRequest;
import com.alessandropesole.bonwoapp.exercise.application.mapper.ExerciseDtoMapper;
import com.alessandropesole.bonwoapp.exercise.application.mapper.MuscleEntryDtoMapper;
import com.alessandropesole.bonwoapp.exercise.domain.model.Exercise;
import com.alessandropesole.bonwoapp.exercise.domain.model.ExerciseFilter;
import com.alessandropesole.bonwoapp.exercise.domain.model.MuscleEntry;
import com.alessandropesole.bonwoapp.exercise.domain.model.MuscleSummary;
import com.alessandropesole.bonwoapp.exercise.domain.port.in.ExerciseUseCase;
import com.alessandropesole.bonwoapp.exercise.domain.port.out.ExerciseRepository;
import com.alessandropesole.bonwoapp.media.application.service.MediaResolver;
import com.alessandropesole.bonwoapp.media.application.service.MediaService;
import com.alessandropesole.bonwoapp.shared.infrastructure.exception.ForbiddenOperationException;
import com.alessandropesole.bonwoapp.shared.infrastructure.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ExerciseService implements ExerciseUseCase {

    private final ExerciseRepository exerciseRepository;
    private final EquipmentRepository equipmentRepository;
    private final ActivityRepository activityRepository;
    private final TrainingGoalRepository trainingGoalRepository;
    private final MuscleSubGroupRepository muscleSubGroupRepository;
    private final CatalogValidator catalogValidator;
    private final MediaService mediaService;
    private final MediaResolver mediaResolver;
    private final MuscleSummaryCalculator muscleSummaryCalculator;

    @Override
    public ExerciseResponse create(CreateExerciseRequest req, Long ownerId) {
        catalogValidator.validate(req.equipmentIds(), req.activityIds(), req.trainingGoalIds());

        var muscles = MuscleEntryDtoMapper.toDomainList(req.muscles());
        var muscleSummary = muscleSummaryCalculator.calculate(muscles);

        Long thumbnailId = req.thumbnailUploadToken() != null
                ? mediaService.claimImage(req.thumbnailUploadToken(), ownerId) : null;
        Long mainVideoId = req.mainVideoUploadToken() != null
                ? mediaService.claimVideo(req.mainVideoUploadToken(), ownerId) : null;

        Exercise saved = exerciseRepository.save(Exercise.create(
                ownerId, req.title(), req.level(),
                thumbnailId, mainVideoId,
                req.description(), req.instructions(),
                muscles, muscleSummary,
                req.equipmentIds(), req.activityIds(), req.trainingGoalIds()
        ));
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ExerciseResponse getById(Long id, Long ownerId) {
        return toResponse(findOwned(id, ownerId));
    }

    @Override
    public ExerciseResponse update(Long id, UpdateExerciseRequest req, Long ownerId) {
        Exercise exercise = findOwned(id, ownerId);

        if (req.equipmentIds() != null || req.activityIds() != null || req.trainingGoalIds() != null) {
            catalogValidator.validate(
                    req.equipmentIds() != null ? req.equipmentIds() : exercise.getEquipmentIds(),
                    req.activityIds() != null ? req.activityIds() : exercise.getActivityIds(),
                    req.trainingGoalIds() != null ? req.trainingGoalIds() : exercise.getTrainingGoalIds());
        }

        List<MuscleEntry> newMuscles = req.muscles() != null
                ? MuscleEntryDtoMapper.toDomainList(req.muscles()) : null;
        MuscleSummary newMuscleSummary = newMuscles != null
                ? muscleSummaryCalculator.calculate(newMuscles) : null;

        Long oldMainVideoId = exercise.getMainVideoId();

        Long newThumbnailId = req.thumbnailUploadToken() != null
                ? mediaService.claimImage(req.thumbnailUploadToken(), ownerId) : null;
        Long newMainVideoId = req.mainVideoUploadToken() != null
                ? mediaService.claimVideo(req.mainVideoUploadToken(), ownerId) : null;

        exercise.update(
                req.title(), req.level(),
                newThumbnailId, req.removeThumbnail(),
                newMainVideoId, req.removeMainVideo(),
                req.description(), req.instructions(),
                newMuscles, newMuscleSummary,
                req.equipmentIds(), req.activityIds(), req.trainingGoalIds()
        );
        Exercise saved = exerciseRepository.save(exercise);

        if (newMainVideoId != null || req.removeMainVideo())
            mediaService.deleteVideoIfOwner(oldMainVideoId, ownerId);

        return toResponse(saved);
    }

    @Override
    public void delete(Long id, Long ownerId) {
        Exercise exercise = findOwned(id, ownerId);
        mediaService.deleteVideoIfOwner(exercise.getMainVideoId(), ownerId);
        exerciseRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExerciseResponse> listMine(Long ownerId, ExerciseFilter filter, Pageable pageable) {
        Set<Long> muscleSubGroupIds = resolveMuscleSubGroupIds(filter);
        return exerciseRepository.findByOwner(ownerId, muscleSubGroupIds,
                        filter.equipmentIds(), filter.activityIds(), filter.trainingGoalIds(), pageable)
                .map(this::toResponse);
    }

    private Set<Long> resolveMuscleSubGroupIds(ExerciseFilter filter) {
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

    private Exercise findOwned(Long id, Long ownerId) {
        Exercise exercise = exerciseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exercise", id));
        if (!exercise.isOwnedBy(ownerId))
            throw new ForbiddenOperationException("You don't own this exercise");
        return exercise;
    }

    private ExerciseResponse toResponse(Exercise e) {
        List<EquipmentResponse> equipment = e.getEquipmentIds().isEmpty() ? List.of()
                : equipmentRepository.findAllById(e.getEquipmentIds()).stream()
                .map(eq -> EquipmentDtoMapper.toResponse(eq,
                        mediaResolver.resolveImage(eq.getIconId())))
                .toList();

        List<ActivityResponse> activities = e.getActivityIds().isEmpty() ? List.of()
                : activityRepository.findAllById(e.getActivityIds()).stream()
                .map(a -> ActivityDtoMapper.toResponse(a,
                        mediaResolver.resolveImage(a.getIconId())))
                .toList();

        List<TrainingGoalResponse> trainingGoals = e.getTrainingGoalIds().isEmpty() ? List.of()
                : trainingGoalRepository.findAllById(e.getTrainingGoalIds()).stream()
                .map(t -> TrainingGoalDtoMapper.toResponse(t,
                        mediaResolver.resolveImage(t.getIconId())))
                .toList();

        List<MuscleEntryResponse> muscles = resolveMuscleEntries(e.getMuscles());

        return ExerciseDtoMapper.toResponse(
                e, equipment, activities, trainingGoals,
                mediaResolver.resolveImage(e.getThumbnailId()),
                mediaResolver.resolveVideo(e.getMainVideoId()),
                muscles
        );
    }


    private List<MuscleEntryResponse> resolveMuscleEntries(List<MuscleEntry> entries) {
        if (entries == null || entries.isEmpty()) return List.of();
        var subGroupIds = entries.stream().map(MuscleEntry::getSubGroupId).toList();
        var subGroupMap = muscleSubGroupRepository.findAllById(subGroupIds).stream()
                .collect(Collectors.toMap(
                        s -> s.getId(),
                        s -> MuscleSubGroupDtoMapper.toResponse(s,
                                mediaResolver.resolveImage(s.getIconId()))
                ));
        return entries.stream()
                .map(m -> MuscleEntryDtoMapper.toResponse(m, subGroupMap.get(m.getSubGroupId())))
                .toList();
    }
}
