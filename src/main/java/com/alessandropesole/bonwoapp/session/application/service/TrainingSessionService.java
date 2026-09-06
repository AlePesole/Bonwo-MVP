package com.alessandropesole.bonwoapp.session.application.service;

import com.alessandropesole.bonwoapp.exercise.application.dto.ExerciseResponse;
import com.alessandropesole.bonwoapp.exercise.application.service.MuscleSummaryCalculator;
import com.alessandropesole.bonwoapp.exercise.application.service.publication.ExerciseVisibilityResolver;
import com.alessandropesole.bonwoapp.exercise.domain.model.Exercise;
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
import com.alessandropesole.bonwoapp.session.application.dto.TrainingSlotResponse;
import com.alessandropesole.bonwoapp.session.application.dto.UpdateTrainingSessionRequest;
import com.alessandropesole.bonwoapp.session.application.mapper.TrainingSessionDtoMapper;
import com.alessandropesole.bonwoapp.session.application.mapper.TrainingSlotDtoMapper;
import com.alessandropesole.bonwoapp.session.domain.exception.SessionAlreadyInProgressException;
import com.alessandropesole.bonwoapp.session.domain.model.SessionStatus;
import com.alessandropesole.bonwoapp.session.domain.model.TrainingSession;
import com.alessandropesole.bonwoapp.session.domain.model.TrainingSet;
import com.alessandropesole.bonwoapp.session.domain.model.TrainingSlot;
import com.alessandropesole.bonwoapp.session.domain.port.in.TrainingSessionUseCase;
import com.alessandropesole.bonwoapp.session.domain.port.out.TrainingSessionRepository;
import com.alessandropesole.bonwoapp.shared.infrastructure.exception.ForbiddenOperationException;
import com.alessandropesole.bonwoapp.shared.infrastructure.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TrainingSessionService implements TrainingSessionUseCase {

    private final TrainingSessionRepository trainingSessionRepository;
    private final RoutineRepository routineRepository;
    private final ExerciseRepository exerciseRepository;
    private final ExerciseUseCase exerciseUseCase;
    private final ExerciseVisibilityResolver exerciseVisibilityResolver;
    private final MuscleSummaryCalculator muscleSummaryCalculator;

    @Override
    public TrainingSessionResponse start(StartTrainingSessionRequest req, Long ownerId) {
        if (trainingSessionRepository.existsByOwnerAndStatus(ownerId, SessionStatus.IN_PROGRESS))
            throw new SessionAlreadyInProgressException(
                    "You already have a training session in progress. Complete or delete it before starting a new one.");

        Routine routine = routineRepository.findById(req.routineId())
                .orElseThrow(() -> new ResourceNotFoundException("Routine", req.routineId()));
        if (!routine.isOwnedBy(ownerId))
            throw new ForbiddenOperationException("You don't own this routine");

        List<TrainingSlot> slots = routine.getSlots().stream()
                .map(TrainingSessionService::cloneSlotForSession)
                .toList();
        validateSlotExercisesAreAccessible(slots, ownerId);
        MuscleSummary muscleSummary = resolveAndAggregateMuscleSummary(slots, ownerId);

        TrainingSession session = TrainingSession.start(
                ownerId, routine.getId(), routine.getTitle(), routine.getTrainingProgramId(), slots);
        session.applyMuscleSummary(muscleSummary);

        TrainingSession saved = trainingSessionRepository.save(session);
        return toResponse(saved, ownerId);
    }

    @Override
    @Transactional(readOnly = true)
    public TrainingSessionResponse getById(Long id, Long ownerId) {
        return toResponse(findOwned(id, ownerId), ownerId);
    }

    @Override
    public TrainingSessionResponse update(Long id, UpdateTrainingSessionRequest req, Long ownerId) {
        TrainingSession session = findOwned(id, ownerId);

        List<TrainingSlot> newSlots = req.slots() != null
                ? TrainingSlotDtoMapper.toDomainList(req.slots()) : null;
        if (newSlots != null) validateSlotExercisesAreAccessible(newSlots, ownerId);
        MuscleSummary newMuscleSummary = newSlots != null
                ? resolveAndAggregateMuscleSummary(newSlots, ownerId) : null;

        session.update(newSlots, req.finalNote());
        if (newMuscleSummary != null) session.applyMuscleSummary(newMuscleSummary);

        TrainingSession saved = trainingSessionRepository.save(session);
        return toResponse(saved, ownerId);
    }

    @Override
    public TrainingSessionResponse complete(Long id, CompleteTrainingSessionRequest req, Long ownerId) {
        TrainingSession session = findOwned(id, ownerId);
        session.complete(req.finalNote());
        TrainingSession saved = trainingSessionRepository.save(session);
        return toResponse(saved, ownerId);
    }

    @Override
    public void delete(Long id, Long ownerId) {
        findOwned(id, ownerId);
        trainingSessionRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TrainingSessionResponse> listMine(Long ownerId, Pageable pageable) {
        return trainingSessionRepository.findByOwner(ownerId, pageable)
                .map(s -> toResponse(s, ownerId));
    }

    private static TrainingSlot cloneSlotForSession(ExerciseSlot slot) {
        List<TrainingSet> sets = slot.getSets().stream()
                .map(TrainingSessionService::cloneSetForSession)
                .toList();
        return TrainingSlot.create(slot.getExerciseId(), slot.getPosition(), sets, slot.getRestBetweenSets());
    }

    private static TrainingSet cloneSetForSession(SetConfig s) {
        return switch (s.getType()) {
            case REPS -> TrainingSet.reps(s.getReps(), s.getWeightKg(), s.getWeightMode(), false);
            case TIMED -> TrainingSet.timed(s.getDuration(), s.getWeightKg(), s.getWeightMode(), false);
            case AMRAP -> TrainingSet.amrap(s.getDuration(), false);
            case FAILURE -> TrainingSet.toFailure(s.getWeightKg(), s.getWeightMode(), false);
        };
    }

    private TrainingSession findOwned(Long id, Long ownerId) {
        TrainingSession session = trainingSessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TrainingSession", id));
        if (!session.isOwnedBy(ownerId))
            throw new ForbiddenOperationException("You don't own this training session");
        return session;
    }

    /** A session can only reference exercises the caller owns, or exercises published and visible
     *  to anyone — same rule as Routine. */
    private void validateSlotExercisesAreAccessible(List<TrainingSlot> slots, Long ownerId) {
        for (TrainingSlot slot : slots) {
            Exercise exercise = exerciseRepository.findById(slot.getExerciseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Exercise", slot.getExerciseId()));
            if (!exerciseVisibilityResolver.isVisible(exercise, ownerId))
                throw new ForbiddenOperationException(
                        "Exercise " + slot.getExerciseId() + " is not accessible to you");
        }
    }

    private MuscleSummary resolveAndAggregateMuscleSummary(List<TrainingSlot> slots, Long ownerId) {
        List<MuscleSummary> summaries = slots.stream()
                .map(slot -> MuscleSummary.of(exerciseUseCase.getById(slot.getExerciseId(), ownerId).muscleSummary()))
                .toList();
        return muscleSummaryCalculator.aggregate(summaries);
    }

    private TrainingSessionResponse toResponse(TrainingSession s, Long ownerId) {
        Set<Long> exerciseIds = s.getSlots().stream()
                .map(TrainingSlot::getExerciseId)
                .collect(Collectors.toSet());
        Map<Long, ExerciseResponse> exercises = exerciseUseCase.getVisibleByIds(exerciseIds, ownerId);

        List<TrainingSlotResponse> slots = s.getSlots().stream()
                .map(slot -> TrainingSlotDtoMapper.toResponse(slot, exercises.get(slot.getExerciseId())))
                .toList();
        return TrainingSessionDtoMapper.toResponse(s, slots);
    }
}
