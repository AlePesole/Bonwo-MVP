package com.alessandropesole.bonwoapp.user.application.service;

import com.alessandropesole.bonwoapp.exercise.domain.model.Exercise;
import com.alessandropesole.bonwoapp.exercise.domain.model.Level;
import com.alessandropesole.bonwoapp.exercise.domain.port.out.ExerciseRepository;
import com.alessandropesole.bonwoapp.routine.domain.model.ExerciseSlot;
import com.alessandropesole.bonwoapp.routine.domain.model.Routine;
import com.alessandropesole.bonwoapp.routine.domain.model.SetConfig;
import com.alessandropesole.bonwoapp.routine.domain.port.out.RoutineRepository;
import com.alessandropesole.bonwoapp.support.AbstractIntegrationTest;
import com.alessandropesole.bonwoapp.user.domain.model.User;
import com.alessandropesole.bonwoapp.user.domain.port.out.RefreshTokenRepository;
import com.alessandropesole.bonwoapp.user.domain.model.RefreshToken;
import com.alessandropesole.bonwoapp.user.domain.port.out.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class UserAccountDeletionServiceIT extends AbstractIntegrationTest {

    @Autowired
    private UserAccountDeletionService deletionService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ExerciseRepository exerciseRepository;
    @Autowired
    private RoutineRepository routineRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    void deleteAccount_removesUserAndAllOwnedContent() {
        User user = userRepository.save(User.register("delete-me@example.com", "hash", "deleteme"));
        Long userId = user.getId();

        Exercise exercise = exerciseRepository.save(Exercise.create(
                userId, "Bench Press", Level.INTERMEDIATE,
                null, null, null, null,
                List.of(), null, Set.of(), Set.of(), Set.of()));

        ExerciseSlot slot = ExerciseSlot.create(exercise.getId(), 1,
                List.of(SetConfig.reps(10, null, null)), null);
        Routine routine = routineRepository.save(Routine.create(
                userId, "Push Day", null, Level.INTERMEDIATE, null,
                List.of(slot), null, Set.of(), Set.of(), Set.of()));

        refreshTokenRepository.save(RefreshToken.issue(userId, "token-id", Instant.now().plusSeconds(3600)));

        deletionService.deleteAccount(userId);

        assertThat(userRepository.findById(userId)).isEmpty();
        assertThat(exerciseRepository.findById(exercise.getId())).isEmpty();
        assertThat(routineRepository.findById(routine.getId())).isEmpty();
    }
}
