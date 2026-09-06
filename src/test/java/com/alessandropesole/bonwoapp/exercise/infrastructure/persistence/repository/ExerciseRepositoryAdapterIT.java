package com.alessandropesole.bonwoapp.exercise.infrastructure.persistence.repository;

import com.alessandropesole.bonwoapp.exercise.domain.model.Exercise;
import com.alessandropesole.bonwoapp.exercise.domain.model.Level;
import com.alessandropesole.bonwoapp.exercise.domain.model.MuscleEntry;
import com.alessandropesole.bonwoapp.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ExerciseRepositoryAdapter.class)
class ExerciseRepositoryAdapterIT extends AbstractIntegrationTest {

    @Autowired
    private ExerciseRepositoryAdapter exerciseRepository;

    @Test
    void save_persistsAndReloadsExercise() {
        Exercise saved = exerciseRepository.save(Exercise.create(
                1L, "Bench Press", Level.INTERMEDIATE,
                10L, 20L, "desc", "instructions",
                List.of(MuscleEntry.of(4L, 0.8)), null,
                Set.of(1L), Set.of(2L), Set.of(3L)));

        assertThat(saved.getId()).isNotNull();

        Exercise reloaded = exerciseRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getTitle()).isEqualTo("Bench Press");
        assertThat(reloaded.getOwnerId()).isEqualTo(1L);
        assertThat(reloaded.getLevel()).isEqualTo(Level.INTERMEDIATE);
        assertThat(reloaded.getThumbnailId()).isEqualTo(10L);
        assertThat(reloaded.getMainVideoId()).isEqualTo(20L);
        assertThat(reloaded.getMuscles()).hasSize(1);
        assertThat(reloaded.getMuscles().get(0).getSubGroupId()).isEqualTo(4L);
        assertThat(reloaded.getEquipmentIds()).containsExactly(1L);
        assertThat(reloaded.getActivityIds()).containsExactly(2L);
        assertThat(reloaded.getTrainingGoalIds()).containsExactly(3L);
    }

    @Test
    void findByOwner_returnsOnlyExercisesOwnedByThatUser() {
        exerciseRepository.save(basicExercise(1L, "Squat"));
        exerciseRepository.save(basicExercise(2L, "Deadlift"));

        var page = exerciseRepository.findByOwner(1L, Set.of(), Set.of(), Set.of(), Set.of(),
                null, PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(Exercise::getTitle).containsExactly("Squat");
    }

    @Test
    void findByOwner_filtersByMuscleSubGroupId() {
        exerciseRepository.save(exerciseWithMuscle(1L, "Bench Press", 4L));
        exerciseRepository.save(exerciseWithMuscle(1L, "Squat", 21L));

        var page = exerciseRepository.findByOwner(1L, Set.of(4L), Set.of(), Set.of(), Set.of(),
                null, PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(Exercise::getTitle).containsExactly("Bench Press");
    }

    @Test
    void findByOwner_filtersByEquipmentIdWithOrSemantics() {
        exerciseRepository.save(exerciseWithEquipment(1L, "Bench Press", Set.of(1L)));
        exerciseRepository.save(exerciseWithEquipment(1L, "Pull Up", Set.of(2L)));
        exerciseRepository.save(exerciseWithEquipment(1L, "Push Up", Set.of(3L)));

        var page = exerciseRepository.findByOwner(1L, Set.of(), Set.of(1L, 2L), Set.of(), Set.of(),
                null, PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(Exercise::getTitle)
                .containsExactlyInAnyOrder("Bench Press", "Pull Up");
    }

    @Test
    void findByOwner_doesNotDuplicateExerciseWithMultipleMatchingJoinRows() {
        exerciseRepository.save(exerciseWithEquipment(1L, "Dumbbell Fly", Set.of(1L, 3L)));

        var page = exerciseRepository.findByOwner(1L, Set.of(), Set.of(1L, 3L), Set.of(), Set.of(),
                null, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void findAllById_returnsOnlyMatchingExercises() {
        Exercise saved1 = exerciseRepository.save(basicExercise(1L, "Squat"));
        exerciseRepository.save(basicExercise(1L, "Deadlift"));
        Exercise saved3 = exerciseRepository.save(basicExercise(1L, "Bench Press"));

        var result = exerciseRepository.findAllById(Set.of(saved1.getId(), saved3.getId()));

        assertThat(result).extracting(Exercise::getTitle)
                .containsExactlyInAnyOrder("Squat", "Bench Press");
    }

    @Test
    void deleteById_removesExercise() {
        Exercise saved = exerciseRepository.save(basicExercise(1L, "Lunges"));

        exerciseRepository.deleteById(saved.getId());

        assertThat(exerciseRepository.findById(saved.getId())).isEmpty();
    }

    private Exercise basicExercise(Long ownerId, String title) {
        return Exercise.create(ownerId, title, Level.INTERMEDIATE,
                null, null, null, null,
                List.of(), null, Set.of(), Set.of(), Set.of());
    }

    private Exercise exerciseWithMuscle(Long ownerId, String title, Long subGroupId) {
        return Exercise.create(ownerId, title, Level.INTERMEDIATE,
                null, null, null, null,
                List.of(MuscleEntry.of(subGroupId, 0.7)), null, Set.of(), Set.of(), Set.of());
    }

    private Exercise exerciseWithEquipment(Long ownerId, String title, Set<Long> equipmentIds) {
        return Exercise.create(ownerId, title, Level.INTERMEDIATE,
                null, null, null, null,
                List.of(), null, equipmentIds, Set.of(), Set.of());
    }
}
