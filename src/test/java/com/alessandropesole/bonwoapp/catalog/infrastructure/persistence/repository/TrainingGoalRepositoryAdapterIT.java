package com.alessandropesole.bonwoapp.catalog.infrastructure.persistence.repository;

import com.alessandropesole.bonwoapp.catalog.domain.model.TrainingGoal;
import com.alessandropesole.bonwoapp.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TrainingGoalRepositoryAdapter.class)
class TrainingGoalRepositoryAdapterIT extends AbstractIntegrationTest {

    @Autowired
    private TrainingGoalRepositoryAdapter trainingGoalRepository;

    @Test
    void save_persistsAndReloadsTrainingGoal() {
        TrainingGoal saved = trainingGoalRepository.save(
                TrainingGoal.create("Weight loss", "Lose fat", 1L));

        assertThat(saved.getId()).isNotNull();

        TrainingGoal reloaded = trainingGoalRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("Weight loss");
        assertThat(reloaded.getDetail()).isEqualTo("Lose fat");
        assertThat(reloaded.getIconId()).isEqualTo(1L);
    }

    @Test
    void existsByName_reflectsPersistedState() {
        trainingGoalRepository.save(TrainingGoal.create("Muscle gain", "Build muscle", 1L));

        assertThat(trainingGoalRepository.existsByName("Muscle gain")).isTrue();
        assertThat(trainingGoalRepository.existsByName("Unknown")).isFalse();
    }

    @Test
    void save_rejectsDuplicateNameAtDatabaseLevel() {
        trainingGoalRepository.save(TrainingGoal.create("Endurance", "Build stamina", 1L));

        assertThatThrownBy(() ->
                trainingGoalRepository.save(TrainingGoal.create("Endurance", "Other detail", 2L)));
    }

    @Test
    void findAllById_returnsOnlyMatchingTrainingGoals() {
        TrainingGoal t1 = trainingGoalRepository.save(TrainingGoal.create("Flexibility", "Stretch", 1L));
        TrainingGoal t2 = trainingGoalRepository.save(TrainingGoal.create("Balance", "Stability", 2L));
        trainingGoalRepository.save(TrainingGoal.create("Speed", "Sprint", 3L));

        var result = trainingGoalRepository.findAllById(Set.of(t1.getId(), t2.getId()));

        assertThat(result).hasSize(2)
                .extracting(TrainingGoal::getName)
                .containsExactlyInAnyOrder("Flexibility", "Balance");
    }

    @Test
    void deleteById_removesTrainingGoal() {
        TrainingGoal saved = trainingGoalRepository.save(
                TrainingGoal.create("Rehab", "Recovery", 1L));

        trainingGoalRepository.deleteById(saved.getId());

        assertThat(trainingGoalRepository.findById(saved.getId())).isEmpty();
    }
}