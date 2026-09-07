package com.alessandropesole.bonwoapp.user.application.service;

import com.alessandropesole.bonwoapp.exercise.application.dto.publication.ExercisePublicationResponse;
import com.alessandropesole.bonwoapp.exercise.domain.model.Exercise;
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
import com.alessandropesole.bonwoapp.user.domain.model.AccountStatus;
import com.alessandropesole.bonwoapp.user.domain.model.User;
import com.alessandropesole.bonwoapp.user.domain.model.UserProfile;
import com.alessandropesole.bonwoapp.user.domain.model.UserRole;
import com.alessandropesole.bonwoapp.user.domain.port.out.RefreshTokenRepository;
import com.alessandropesole.bonwoapp.user.domain.port.out.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAccountDeletionServiceTest {

    private static final Long USER_ID = 1L;

    @Mock private UserRepository userRepository;
    @Mock private TrainingSessionRepository trainingSessionRepository;
    @Mock private TrainingSessionUseCase trainingSessionUseCase;
    @Mock private TrainingProgramRepository trainingProgramRepository;
    @Mock private TrainingProgramUseCase trainingProgramUseCase;
    @Mock private RoutineRepository routineRepository;
    @Mock private RoutineUseCase routineUseCase;
    @Mock private ExercisePublicationUseCase exercisePublicationUseCase;
    @Mock private ExerciseRepository exerciseRepository;
    @Mock private ExerciseUseCase exerciseUseCase;
    @Mock private MediaService mediaService;
    @Mock private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private UserAccountDeletionService service;

    private static User existingUser() {
        return User.reconstitute(USER_ID, "user@example.com", "hash", "johndoe",
                UserRole.USER, AccountStatus.ACTIVE, UserProfile.empty(), Instant.now());
    }

    private static <T> Page<T> onePage(T item) {
        return new PageImpl<>(List.of(item), PageRequest.of(0, 50), 1);
    }

    private static <T> Page<T> emptyPage() {
        return new PageImpl<>(List.of(), PageRequest.of(0, 50), 0);
    }

    @Test
    void deleteAccount_throwsWhenUserNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteAccount(USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository, never()).deleteById(any());
    }

    @Test
    void deleteAccount_deletesEveryOwnedResourceThenTheUser() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser()));

        TrainingSession session = mock(TrainingSession.class);
        when(session.getId()).thenReturn(10L);
        when(trainingSessionRepository.findByOwner(eq(USER_ID), any(Pageable.class)))
                .thenReturn(onePage(session), emptyPage());

        TrainingProgram program = mock(TrainingProgram.class);
        when(program.getId()).thenReturn(20L);
        when(trainingProgramRepository.findByOwner(eq(USER_ID), anySet(), anySet(), anySet(), any(), any(Pageable.class)))
                .thenReturn(onePage(program), emptyPage());

        Routine routine = mock(Routine.class);
        when(routine.getId()).thenReturn(30L);
        when(routineRepository.findByOwner(eq(USER_ID), anySet(), anySet(), anySet(), anySet(), any(), any(Pageable.class)))
                .thenReturn(onePage(routine), emptyPage());

        Exercise exercise = mock(Exercise.class);
        when(exercise.getId()).thenReturn(40L);
        when(exerciseRepository.findByOwner(eq(USER_ID), anySet(), anySet(), anySet(), anySet(), any(), any(Pageable.class)))
                .thenReturn(onePage(exercise), emptyPage());

        ExercisePublicationResponse publication = mock(ExercisePublicationResponse.class);
        when(publication.id()).thenReturn(50L);
        when(exercisePublicationUseCase.listMine(eq(USER_ID), any(), any(Pageable.class)))
                .thenReturn(onePage(publication), emptyPage());

        ExercisePublicationResponse liked = mock(ExercisePublicationResponse.class);
        when(liked.id()).thenReturn(60L);
        when(exercisePublicationUseCase.listLiked(eq(USER_ID), any(), any(Pageable.class)))
                .thenReturn(onePage(liked), emptyPage());

        ExercisePublicationResponse saved = mock(ExercisePublicationResponse.class);
        when(saved.id()).thenReturn(70L);
        when(exercisePublicationUseCase.listSaved(eq(USER_ID), any(), any(Pageable.class)))
                .thenReturn(onePage(saved), emptyPage());

        service.deleteAccount(USER_ID);

        verify(trainingSessionUseCase).delete(10L, USER_ID);
        verify(trainingProgramUseCase).delete(20L, USER_ID);
        verify(routineUseCase).delete(30L, USER_ID);
        verify(exercisePublicationUseCase).delete(50L, USER_ID);
        verify(exerciseUseCase).delete(40L, USER_ID);
        verify(exercisePublicationUseCase).unlike(60L, USER_ID);
        verify(exercisePublicationUseCase).unsave(70L, USER_ID);
        verify(mediaService).deleteAllVideosOwnedBy(USER_ID);
        verify(refreshTokenRepository).deleteAllByUserId(USER_ID);
        verify(userRepository).deleteById(USER_ID);
    }
}
