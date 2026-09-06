package com.alessandropesole.bonwoapp.exercise.application.service;

import com.alessandropesole.bonwoapp.catalog.application.dto.ActivityResponse;
import com.alessandropesole.bonwoapp.catalog.application.dto.EquipmentResponse;
import com.alessandropesole.bonwoapp.catalog.application.dto.MuscleSubGroupResponse;
import com.alessandropesole.bonwoapp.catalog.application.dto.TrainingGoalResponse;
import com.alessandropesole.bonwoapp.catalog.application.mapper.ActivityDtoMapper;
import com.alessandropesole.bonwoapp.catalog.application.mapper.EquipmentDtoMapper;
import com.alessandropesole.bonwoapp.catalog.application.mapper.MuscleSubGroupDtoMapper;
import com.alessandropesole.bonwoapp.catalog.application.mapper.TrainingGoalDtoMapper;
import com.alessandropesole.bonwoapp.catalog.application.service.CatalogValidator;
import com.alessandropesole.bonwoapp.catalog.domain.model.Activity;
import com.alessandropesole.bonwoapp.catalog.domain.model.Equipment;
import com.alessandropesole.bonwoapp.catalog.domain.model.MuscleSubGroup;
import com.alessandropesole.bonwoapp.catalog.domain.model.TrainingGoal;
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
import com.alessandropesole.bonwoapp.exercise.application.service.publication.ExerciseVisibilityResolver;
import com.alessandropesole.bonwoapp.exercise.domain.model.Exercise;
import com.alessandropesole.bonwoapp.exercise.domain.model.ExerciseFilter;
import com.alessandropesole.bonwoapp.exercise.domain.model.MuscleEntry;
import com.alessandropesole.bonwoapp.exercise.domain.model.MuscleSummary;
import com.alessandropesole.bonwoapp.exercise.domain.port.in.ExerciseUseCase;
import com.alessandropesole.bonwoapp.exercise.domain.port.out.ExerciseRepository;
import com.alessandropesole.bonwoapp.media.application.dto.ImageResponse;
import com.alessandropesole.bonwoapp.media.application.dto.VideoResponse;
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
import java.util.Map;
import java.util.Objects;
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
    private final ExerciseVisibilityResolver exerciseVisibilityResolver;

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
        return toResponse(findVisible(id, ownerId));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, ExerciseResponse> getVisibleByIds(Set<Long> ids, Long viewerId) {
        if (ids == null || ids.isEmpty()) return Map.of();

        List<Exercise> found = exerciseRepository.findAllById(ids);
        if (found.isEmpty()) return Map.of();

        Map<Long, Boolean> visibility = exerciseVisibilityResolver.isVisibleBulk(found, viewerId);
        List<Exercise> visible = found.stream()
                .filter(e -> Boolean.TRUE.equals(visibility.get(e.getId())))
                .toList();

        return toResponseBulk(visible).stream()
                .collect(Collectors.toMap(ExerciseResponse::id, r -> r));
    }

    @Override
    public ExerciseResponse update(Long id, UpdateExerciseRequest req, Long ownerId) {
        Exercise exercise = findOwned(id, ownerId);
        requireNotPublished(exercise);

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
        requireNotPublished(exercise);
        mediaService.deleteVideoIfOwner(exercise.getMainVideoId(), ownerId);
        exerciseRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExerciseResponse> listMine(Long ownerId, ExerciseFilter filter, Pageable pageable) {
        Set<Long> muscleSubGroupIds = resolveMuscleSubGroupIds(filter);
        return exerciseRepository.findByOwner(ownerId, muscleSubGroupIds,
                        filter.equipmentIds(), filter.activityIds(), filter.trainingGoalIds(), filter.title(), pageable)
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

    private Exercise findVisible(Long id, Long viewerId) {
        Exercise exercise = exerciseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exercise", id));
        if (!exerciseVisibilityResolver.isVisible(exercise, viewerId))
            throw new ForbiddenOperationException("You don't own this exercise");
        return exercise;
    }

    private void requireNotPublished(Exercise exercise) {
        if (exercise.getPublicationId() != null)
            throw new ForbiddenOperationException(
                    "This exercise belongs to a publication — edit it via /exercise-publications instead");
    }

    private ExerciseResponse toResponse(Exercise e) {
        return toResponseBulk(List.of(e)).get(0);
    }

    /** Batch-resolves equipment/activity/trainingGoal/muscle-subgroup/media for any number of
     *  exercises in ~6 queries total, instead of ~6 queries PER exercise — this is what fixes the
     *  N+1 that routine and training-session responses used to trigger per slot. */
    private List<ExerciseResponse> toResponseBulk(List<Exercise> exercises) {
        if (exercises.isEmpty()) return List.of();

        Set<Long> equipmentIds = exercises.stream().flatMap(e -> e.getEquipmentIds().stream()).collect(Collectors.toSet());
        Set<Long> activityIds = exercises.stream().flatMap(e -> e.getActivityIds().stream()).collect(Collectors.toSet());
        Set<Long> trainingGoalIds = exercises.stream().flatMap(e -> e.getTrainingGoalIds().stream()).collect(Collectors.toSet());
        Set<Long> subGroupIds = exercises.stream()
                .flatMap(e -> e.getMuscles().stream())
                .map(MuscleEntry::getSubGroupId)
                .collect(Collectors.toSet());
        Set<Long> thumbnailIds = exercises.stream().map(Exercise::getThumbnailId).collect(Collectors.toSet());
        Set<Long> videoIds = exercises.stream().map(Exercise::getMainVideoId).collect(Collectors.toSet());

        Map<Long, EquipmentResponse> equipmentMap = equipmentIds.isEmpty() ? Map.of()
                : equipmentRepository.findAllById(equipmentIds).stream()
                    .collect(Collectors.toMap(Equipment::getId,
                            eq -> EquipmentDtoMapper.toResponse(eq, mediaResolver.resolveImage(eq.getIconId()))));

        Map<Long, ActivityResponse> activityMap = activityIds.isEmpty() ? Map.of()
                : activityRepository.findAllById(activityIds).stream()
                    .collect(Collectors.toMap(Activity::getId,
                            a -> ActivityDtoMapper.toResponse(a, mediaResolver.resolveImage(a.getIconId()))));

        Map<Long, TrainingGoalResponse> trainingGoalMap = trainingGoalIds.isEmpty() ? Map.of()
                : trainingGoalRepository.findAllById(trainingGoalIds).stream()
                    .collect(Collectors.toMap(TrainingGoal::getId,
                            t -> TrainingGoalDtoMapper.toResponse(t, mediaResolver.resolveImage(t.getIconId()))));

        Map<Long, MuscleSubGroupResponse> subGroupMap = subGroupIds.isEmpty() ? Map.of()
                : muscleSubGroupRepository.findAllById(subGroupIds).stream()
                    .collect(Collectors.toMap(MuscleSubGroup::getId,
                            sg -> MuscleSubGroupDtoMapper.toResponse(sg, mediaResolver.resolveImage(sg.getIconId()))));

        Map<Long, ImageResponse> thumbnailMap = mediaResolver.resolveImages(thumbnailIds);
        Map<Long, VideoResponse> videoMap = mediaResolver.resolveVideos(videoIds);

        return exercises.stream().map(e -> {
            List<EquipmentResponse> equipment = e.getEquipmentIds().stream()
                    .map(equipmentMap::get).filter(Objects::nonNull).toList();
            List<ActivityResponse> activities = e.getActivityIds().stream()
                    .map(activityMap::get).filter(Objects::nonNull).toList();
            List<TrainingGoalResponse> trainingGoals = e.getTrainingGoalIds().stream()
                    .map(trainingGoalMap::get).filter(Objects::nonNull).toList();
            List<MuscleEntryResponse> muscles = e.getMuscles().stream()
                    .map(m -> MuscleEntryDtoMapper.toResponse(m, subGroupMap.get(m.getSubGroupId())))
                    .toList();

            return ExerciseDtoMapper.toResponse(
                    e, equipment, activities, trainingGoals,
                    thumbnailMap.get(e.getThumbnailId()),
                    videoMap.get(e.getMainVideoId()),
                    muscles
            );
        }).toList();
    }
}
