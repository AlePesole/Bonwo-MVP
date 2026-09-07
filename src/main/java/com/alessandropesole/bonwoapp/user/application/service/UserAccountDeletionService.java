package com.alessandropesole.bonwoapp.user.application.service;

import com.alessandropesole.bonwoapp.exercise.application.dto.publication.ExercisePublicationResponse;
import com.alessandropesole.bonwoapp.exercise.domain.model.Exercise;
import com.alessandropesole.bonwoapp.exercise.domain.model.publication.ExercisePublicationFilter;
import com.alessandropesole.bonwoapp.exercise.domain.port.in.ExerciseUseCase;
import com.alessandropesole.bonwoapp.exercise.domain.port.in.publication.ExercisePublicationUseCase;
import com.alessandropesole.bonwoapp.exercise.domain.port.out.ExerciseRepository;
import com.alessandropesole.bonwoapp.media.application.service.MediaService;
import com.alessandropesole.bonwoapp.program.domain.model.TrainingProgram;
import com.alessandropesole.bonwoapp.program.domain.port.in.TrainingProgramUseCase;
import com.alessandropesole.bonwoapp.program.domain.port.out.TrainingProgramRepository;
import com.alessandropesole.bonwoapp.routine.domain.model.Routine;
import com.alessandropesole.bonwoapp.routine.domain.port.in.RoutineUseCase;
import com.alessandropesole.bonwoapp.routine.domain.port.out.RoutineRepository;
import com.alessandropesole.bonwoapp.session.domain.model.TrainingSession;
import com.alessandropesole.bonwoapp.session.domain.port.in.TrainingSessionUseCase;
import com.alessandropesole.bonwoapp.session.domain.port.out.TrainingSessionRepository;
import com.alessandropesole.bonwoapp.shared.infrastructure.exception.ResourceNotFoundException;
import com.alessandropesole.bonwoapp.user.domain.port.out.RefreshTokenRepository;
import com.alessandropesole.bonwoapp.user.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class UserAccountDeletionService {

    private static final int PAGE_SIZE = 50;

    private final UserRepository userRepository;
    private final TrainingSessionRepository trainingSessionRepository;
    private final TrainingSessionUseCase trainingSessionUseCase;
    private final TrainingProgramRepository trainingProgramRepository;
    private final TrainingProgramUseCase trainingProgramUseCase;
    private final RoutineRepository routineRepository;
    private final RoutineUseCase routineUseCase;
    private final ExercisePublicationUseCase exercisePublicationUseCase;
    private final ExerciseRepository exerciseRepository;
    private final ExerciseUseCase exerciseUseCase;
    private final MediaService mediaService;
    private final RefreshTokenRepository refreshTokenRepository;

    public void deleteAccount(Long userId) {
        if (userRepository.findById(userId).isEmpty()) {
            throw new ResourceNotFoundException("User", userId);
        }

        deleteTrainingSessions(userId);
        deleteTrainingPrograms(userId);
        deleteRoutines(userId);
        deletePublications(userId);
        deleteExercises(userId);
        removeLikes(userId);
        removeSaves(userId);
        mediaService.deleteAllVideosOwnedBy(userId);
        refreshTokenRepository.deleteAllByUserId(userId);
        userRepository.deleteById(userId);
    }

    private void deleteTrainingSessions(Long userId) {
        Page<TrainingSession> page;
        do {
            page = trainingSessionRepository.findByOwner(userId, firstPage());
            page.getContent().forEach(s -> trainingSessionUseCase.delete(s.getId(), userId));
        } while (page.hasNext());
    }

    private void deleteTrainingPrograms(Long userId) {
        Page<TrainingProgram> page;
        do {
            page = trainingProgramRepository.findByOwner(userId, Set.of(), Set.of(), Set.of(), null, firstPage());
            page.getContent().forEach(p -> trainingProgramUseCase.delete(p.getId(), userId));
        } while (page.hasNext());
    }

    private void deleteRoutines(Long userId) {
        Page<Routine> page;
        do {
            page = routineRepository.findByOwner(userId, Set.of(), Set.of(), Set.of(), Set.of(), null, firstPage());
            page.getContent().forEach(r -> routineUseCase.delete(r.getId(), userId));
        } while (page.hasNext());
    }

    private void deleteExercises(Long userId) {
        Page<Exercise> page;
        do {
            page = exerciseRepository.findByOwner(userId, Set.of(), Set.of(), Set.of(), Set.of(), null, firstPage());
            page.getContent().forEach(e -> exerciseUseCase.delete(e.getId(), userId));
        } while (page.hasNext());
    }

    private void deletePublications(Long userId) {
        Page<ExercisePublicationResponse> page;
        do {
            page = exercisePublicationUseCase.listMine(userId, emptyPublicationFilter(), firstPage());
            page.getContent().forEach(p -> exercisePublicationUseCase.delete(p.id(), userId));
        } while (page.hasNext());
    }

    private void removeLikes(Long userId) {
        Page<ExercisePublicationResponse> page;
        do {
            page = exercisePublicationUseCase.listLiked(userId, emptyPublicationFilter(), firstPage());
            page.getContent().forEach(p -> exercisePublicationUseCase.unlike(p.id(), userId));
        } while (page.hasNext());
    }

    private void removeSaves(Long userId) {
        Page<ExercisePublicationResponse> page;
        do {
            page = exercisePublicationUseCase.listSaved(userId, emptyPublicationFilter(), firstPage());
            page.getContent().forEach(p -> exercisePublicationUseCase.unsave(p.id(), userId));
        } while (page.hasNext());
    }

    private static ExercisePublicationFilter emptyPublicationFilter() {
        return new ExercisePublicationFilter(null, null, Set.of(), Set.of(), Set.of(), null, null, null);
    }

    private static Pageable firstPage() {
        return PageRequest.of(0, PAGE_SIZE);
    }
}
