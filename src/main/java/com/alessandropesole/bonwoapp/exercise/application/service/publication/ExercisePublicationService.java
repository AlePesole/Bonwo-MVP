package com.alessandropesole.bonwoapp.exercise.application.service.publication;

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
import com.alessandropesole.bonwoapp.exercise.application.dto.publication.CreatePublicationRequest;
import com.alessandropesole.bonwoapp.exercise.application.dto.publication.ExercisePublicationResponse;
import com.alessandropesole.bonwoapp.exercise.application.dto.ExerciseResponse;
import com.alessandropesole.bonwoapp.exercise.application.dto.MuscleEntryResponse;
import com.alessandropesole.bonwoapp.exercise.application.dto.publication.UpdatePublicationRequest;
import com.alessandropesole.bonwoapp.exercise.application.mapper.ExerciseDtoMapper;
import com.alessandropesole.bonwoapp.exercise.application.mapper.publication.ExercisePublicationDtoMapper;
import com.alessandropesole.bonwoapp.exercise.application.mapper.MuscleEntryDtoMapper;
import com.alessandropesole.bonwoapp.exercise.application.service.MuscleSummaryCalculator;
import com.alessandropesole.bonwoapp.exercise.domain.exception.publication.InvalidExercisePublicationException;
import com.alessandropesole.bonwoapp.exercise.domain.model.Exercise;
import com.alessandropesole.bonwoapp.exercise.domain.model.publication.ExercisePublication;
import com.alessandropesole.bonwoapp.exercise.domain.model.MuscleEntry;
import com.alessandropesole.bonwoapp.exercise.domain.model.MuscleSummary;
import com.alessandropesole.bonwoapp.exercise.domain.model.publication.ExercisePublicationFilter;
import com.alessandropesole.bonwoapp.exercise.domain.model.publication.PublicationSort;
import com.alessandropesole.bonwoapp.exercise.domain.model.publication.PublicationType;
import com.alessandropesole.bonwoapp.exercise.domain.port.in.publication.ExercisePublicationUseCase;
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
import com.alessandropesole.bonwoapp.user.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Creating a publication also creates its Exercise, in one step — there's no "publish an existing
 * exercise" flow (see plan). Deliberately bypasses ExerciseUseCase/ExerciseService: that path is
 * blocked for exercises already belonging to a publication, so routing through it here would
 * self-block. Instead this reuses the same lower-level pieces ExerciseService uses internally
 * (Exercise domain, ExerciseRepository, MuscleSummaryCalculator, CatalogValidator, static mappers).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ExercisePublicationService implements ExercisePublicationUseCase {

    private final ExercisePublicationRepository publicationRepository;
    private final ExerciseRepository exerciseRepository;
    private final ExercisePublicationLikeRepository likeRepository;
    private final ExercisePublicationSaveRepository saveRepository;
    private final ExercisePublicationViewRepository viewRepository;
    private final ExercisePublicationUseRepository useRepository;
    private final EquipmentRepository equipmentRepository;
    private final ActivityRepository activityRepository;
    private final TrainingGoalRepository trainingGoalRepository;
    private final MuscleSubGroupRepository muscleSubGroupRepository;
    private final CatalogValidator catalogValidator;
    private final MediaService mediaService;
    private final MediaResolver mediaResolver;
    private final MuscleSummaryCalculator muscleSummaryCalculator;
    private final UserRepository userRepository;

    @Override
    public ExercisePublicationResponse create(CreatePublicationRequest req, Long authorId) {
        validateAuthorCanPublish(req.type(), authorId);
        catalogValidator.validate(req.equipmentIds(), req.activityIds(), req.trainingGoalIds());
        validatePublicationRules(req);

        var muscles = MuscleEntryDtoMapper.toDomainList(req.muscles());
        var muscleSummary = muscleSummaryCalculator.calculate(muscles);

        Long thumbnailId = req.thumbnailUploadToken() != null
                ? mediaService.claimImage(req.thumbnailUploadToken(), authorId) : null;
        Long mainVideoId = req.mainVideoUploadToken() != null
                ? mediaService.claimVideo(req.mainVideoUploadToken(), authorId) : null;

        Exercise exercise = exerciseRepository.save(Exercise.create(
                authorId, req.title(), req.level(),
                thumbnailId, mainVideoId,
                req.description(), req.instructions(),
                muscles, muscleSummary,
                req.equipmentIds(), req.activityIds(), req.trainingGoalIds(),
                null
        ));

        ExercisePublication publication = publicationRepository.save(
                ExercisePublication.create(exercise.getId(), authorId, req.type(), req.visibility()));

        exercise.assignPublication(publication.getId());
        exercise = exerciseRepository.save(exercise);

        return toResponse(publication, exercise, authorId);
    }

    @Override
    public ExercisePublicationResponse getById(Long id, Long viewerId) {
        ExercisePublication publication = findPublication(id);
        Exercise exercise = findExercise(publication.getExerciseId());

        if (viewerId != null && !viewRepository.exists(id, viewerId)) {
            viewRepository.add(id, viewerId);
            publication.incrementViews();
            publication = publicationRepository.save(publication);
        }

        return toResponse(publication, exercise, viewerId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExercisePublicationResponse> listFeed(ExercisePublicationFilter filter, Long viewerId, Pageable pageable) {
        Set<Long> muscleSubGroupIds = resolveMuscleSubGroupIds(filter);
        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), resolveSort(filter.sort()));
        return publicationRepository.findFeed(filter.type(), muscleSubGroupIds,
                        filter.equipmentIds(), filter.activityIds(), filter.trainingGoalIds(), filter.title(), sorted)
                .map(p -> toResponse(p, findExercise(p.getExerciseId()), viewerId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExercisePublicationResponse> listMine(Long authorId, ExercisePublicationFilter filter, Pageable pageable) {
        Set<Long> muscleSubGroupIds = resolveMuscleSubGroupIds(filter);
        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), resolveSort(filter.sort()));
        return publicationRepository.findByAuthor(authorId, filter.type(), muscleSubGroupIds,
                        filter.equipmentIds(), filter.activityIds(), filter.trainingGoalIds(), filter.title(), sorted)
                .map(p -> toResponse(p, findExercise(p.getExerciseId()), authorId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExercisePublicationResponse> listLiked(Long userId, ExercisePublicationFilter filter, Pageable pageable) {
        Set<Long> muscleSubGroupIds = resolveMuscleSubGroupIds(filter);
        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), resolveSort(filter.sort()));
        return publicationRepository.findLikedByUser(userId, filter.type(), muscleSubGroupIds,
                        filter.equipmentIds(), filter.activityIds(), filter.trainingGoalIds(), filter.title(), sorted)
                .map(p -> toResponse(p, findExercise(p.getExerciseId()), userId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExercisePublicationResponse> listSaved(Long userId, ExercisePublicationFilter filter, Pageable pageable) {
        Set<Long> muscleSubGroupIds = resolveMuscleSubGroupIds(filter);
        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), resolveSort(filter.sort()));
        return publicationRepository.findSavedByUser(userId, filter.type(), muscleSubGroupIds,
                        filter.equipmentIds(), filter.activityIds(), filter.trainingGoalIds(), filter.title(), sorted)
                .map(p -> toResponse(p, findExercise(p.getExerciseId()), userId));
    }

    @Override
    public ExercisePublicationResponse update(Long id, UpdatePublicationRequest req, Long authorId) {
        ExercisePublication publication = findOwnedPublication(id, authorId);
        Exercise exercise = findExercise(publication.getExerciseId());

        if (req.equipmentIds() != null || req.activityIds() != null || req.trainingGoalIds() != null) {
            Set<Long> effectiveEquipmentIds = req.equipmentIds() != null ? req.equipmentIds() : exercise.getEquipmentIds();
            Set<Long> effectiveActivityIds = req.activityIds() != null ? req.activityIds() : exercise.getActivityIds();
            Set<Long> effectiveTrainingGoalIds = req.trainingGoalIds() != null ? req.trainingGoalIds() : exercise.getTrainingGoalIds();
            catalogValidator.validate(effectiveEquipmentIds, effectiveActivityIds, effectiveTrainingGoalIds);
            validateAtLeastOneEach(effectiveEquipmentIds, effectiveActivityIds, effectiveTrainingGoalIds);
        }

        List<MuscleEntry> newMuscles = req.muscles() != null
                ? MuscleEntryDtoMapper.toDomainList(req.muscles()) : null;
        MuscleSummary newMuscleSummary = newMuscles != null
                ? muscleSummaryCalculator.calculate(newMuscles) : null;

        Long newThumbnailId = req.thumbnailUploadToken() != null
                ? mediaService.claimImage(req.thumbnailUploadToken(), authorId) : null;

        exercise.update(
                req.title(), req.level(),
                newThumbnailId, false,
                null, false,
                req.description(), req.instructions(),
                newMuscles, newMuscleSummary,
                req.equipmentIds(), req.activityIds(), req.trainingGoalIds()
        );
        exercise = exerciseRepository.save(exercise);

        publication.updateVisibility(req.visibility());
        publication = publicationRepository.save(publication);

        return toResponse(publication, exercise, authorId);
    }

    @Override
    public void delete(Long id, Long authorId) {
        ExercisePublication publication = findOwnedPublication(id, authorId);
        Exercise exercise = findExercise(publication.getExerciseId());
        mediaService.deleteVideoIfOwner(exercise.getMainVideoId(), authorId);
        publicationRepository.deleteById(id);
        exerciseRepository.deleteById(exercise.getId());
    }

    @Override
    public ExercisePublicationResponse like(Long id, Long userId) {
        ExercisePublication publication = findPublication(id);
        if (!likeRepository.exists(id, userId)) {
            likeRepository.add(id, userId);
            publication.incrementLikes();
            publication = publicationRepository.save(publication);
        }
        return toResponse(publication, findExercise(publication.getExerciseId()), userId);
    }

    @Override
    public ExercisePublicationResponse unlike(Long id, Long userId) {
        ExercisePublication publication = findPublication(id);
        if (likeRepository.exists(id, userId)) {
            likeRepository.remove(id, userId);
            publication.decrementLikes();
            publication = publicationRepository.save(publication);
        }
        return toResponse(publication, findExercise(publication.getExerciseId()), userId);
    }

    @Override
    public ExercisePublicationResponse save(Long id, Long userId) {
        ExercisePublication publication = findPublication(id);
        if (!saveRepository.exists(id, userId)) {
            saveRepository.add(id, userId);
            publication.incrementSaves();
            publication = publicationRepository.save(publication);
        }
        return toResponse(publication, findExercise(publication.getExerciseId()), userId);
    }

    @Override
    public ExercisePublicationResponse unsave(Long id, Long userId) {
        ExercisePublication publication = findPublication(id);
        if (saveRepository.exists(id, userId)) {
            saveRepository.remove(id, userId);
            publication.decrementSaves();
            publication = publicationRepository.save(publication);
        }
        return toResponse(publication, findExercise(publication.getExerciseId()), userId);
    }

    @Override
    public void registerUses(Long routineOwnerId, Long routineId, Set<Long> exerciseIds) {
        if (exerciseIds == null || exerciseIds.isEmpty()) return;

        for (Long exerciseId : exerciseIds) {
            publicationRepository.findByExerciseId(exerciseId).ifPresent(publication -> {
                if (!useRepository.exists(publication.getId(), routineOwnerId)) {
                    useRepository.add(publication.getId(), routineOwnerId, routineId);
                    publication.incrementUses();
                    publicationRepository.save(publication);
                }
            });
        }
    }

    private void validateAuthorCanPublish(PublicationType type, Long authorId) {
        if (type != PublicationType.OFFICIAL) return;
        var author = userRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("User", authorId));
        if (!author.isAdmin())
            throw new ForbiddenOperationException("Only admins can create official publications");
    }

    private void validatePublicationRules(CreatePublicationRequest req) {
        if (req.thumbnailUploadToken() == null || req.thumbnailUploadToken().isBlank())
            throw new InvalidExercisePublicationException("A publication must have a thumbnail");
        if (req.mainVideoUploadToken() == null || req.mainVideoUploadToken().isBlank())
            throw new InvalidExercisePublicationException("A publication must have a video");
        validateAtLeastOneEach(req.equipmentIds(), req.activityIds(), req.trainingGoalIds());
    }

    private void validateAtLeastOneEach(Set<Long> equipmentIds, Set<Long> activityIds, Set<Long> trainingGoalIds) {
        if (equipmentIds == null || equipmentIds.isEmpty())
            throw new InvalidExercisePublicationException("A publication must have at least one equipment");
        if (activityIds == null || activityIds.isEmpty())
            throw new InvalidExercisePublicationException("A publication must have at least one activity");
        if (trainingGoalIds == null || trainingGoalIds.isEmpty())
            throw new InvalidExercisePublicationException("A publication must have at least one training goal");
    }

    private Sort resolveSort(PublicationSort sort) {
        return switch (sort == null ? PublicationSort.RECENT : sort) {
            case RECENT -> Sort.by(Sort.Direction.DESC, "publishedAt");
            case MOST_LIKED -> Sort.by(Sort.Direction.DESC, "likesCount");
            case MOST_VIEWED -> Sort.by(Sort.Direction.DESC, "viewsCount");
            case MOST_USED -> Sort.by(Sort.Direction.DESC, "usesCount");
        };
    }

    private Set<Long> resolveMuscleSubGroupIds(ExercisePublicationFilter filter) {
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

    private ExercisePublication findPublication(Long id) {
        return publicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ExercisePublication", id));
    }

    private ExercisePublication findOwnedPublication(Long id, Long authorId) {
        ExercisePublication publication = findPublication(id);
        if (!publication.isAuthoredBy(authorId))
            throw new ForbiddenOperationException("You don't own this publication");
        return publication;
    }

    private Exercise findExercise(Long exerciseId) {
        return exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new ResourceNotFoundException("Exercise", exerciseId));
    }

    private ExercisePublicationResponse toResponse(ExercisePublication p, Exercise e, Long viewerId) {
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

        ExerciseResponse exerciseResponse = ExerciseDtoMapper.toResponse(
                e, equipment, activities, trainingGoals,
                mediaResolver.resolveImage(e.getThumbnailId()),
                mediaResolver.resolveVideo(e.getMainVideoId()),
                muscles
        );

        boolean likedByMe = viewerId != null && likeRepository.exists(p.getId(), viewerId);
        boolean savedByMe = viewerId != null && saveRepository.exists(p.getId(), viewerId);

        var author = userRepository.findById(p.getAuthorId())
                .orElseThrow(() -> new ResourceNotFoundException("User", p.getAuthorId()));
        var authorAvatar = mediaResolver.resolveImage(author.getProfile().getAvatarId());

        return ExercisePublicationDtoMapper.toResponse(
                p, exerciseResponse, author.getUsername(), authorAvatar, likedByMe, savedByMe);
    }

    private List<MuscleEntryResponse> resolveMuscleEntries(List<MuscleEntry> entries) {
        if (entries == null || entries.isEmpty()) return List.of();
        var subGroupIds = entries.stream().map(MuscleEntry::getSubGroupId).toList();
        var subGroupMap = muscleSubGroupRepository.findAllById(subGroupIds).stream()
                .collect(Collectors.toMap(
                        MuscleSubGroup::getId,
                        s -> MuscleSubGroupDtoMapper.toResponse(s,
                                mediaResolver.resolveImage(s.getIconId()))
                ));
        return entries.stream()
                .map(m -> MuscleEntryDtoMapper.toResponse(m, subGroupMap.get(m.getSubGroupId())))
                .toList();
    }
}
