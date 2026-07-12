package com.alessandropesole.bonwoapp.catalog.infrastructure.persistence.repository;

import com.alessandropesole.bonwoapp.catalog.domain.model.Activity;
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
@Import(ActivityRepositoryAdapter.class)
class ActivityRepositoryAdapterIT extends AbstractIntegrationTest {

    @Autowired
    private ActivityRepositoryAdapter activityRepository;

    @Test
    void save_persistsAndReloadsActivity() {
        Activity saved = activityRepository.save(Activity.create("Running", "Cardio", 1L));

        assertThat(saved.getId()).isNotNull();

        Activity reloaded = activityRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("Running");
        assertThat(reloaded.getDetail()).isEqualTo("Cardio");
        assertThat(reloaded.getIconId()).isEqualTo(1L);
    }

    @Test
    void existsByName_reflectsPersistedState() {
        activityRepository.save(Activity.create("Cycling", "Cardio", 1L));

        assertThat(activityRepository.existsByName("Cycling")).isTrue();
        assertThat(activityRepository.existsByName("Unknown")).isFalse();
    }

    @Test
    void save_rejectsDuplicateNameAtDatabaseLevel() {
        activityRepository.save(Activity.create("Swimming", "Cardio", 1L));

        assertThatThrownBy(() ->
                activityRepository.save(Activity.create("Swimming", "Other detail", 2L)));
    }

    @Test
    void findAllById_returnsOnlyMatchingActivities() {
        Activity a1 = activityRepository.save(Activity.create("Rowing", "Cardio", 1L));
        Activity a2 = activityRepository.save(Activity.create("Boxing", "Combat", 2L));
        activityRepository.save(Activity.create("Yoga", "Flexibility", 3L));

        var result = activityRepository.findAllById(Set.of(a1.getId(), a2.getId()));

        assertThat(result).hasSize(2)
                .extracting(Activity::getName)
                .containsExactlyInAnyOrder("Rowing", "Boxing");
    }

    @Test
    void deleteById_removesActivity() {
        Activity saved = activityRepository.save(Activity.create("Hiking", "Outdoor", 1L));

        activityRepository.deleteById(saved.getId());

        assertThat(activityRepository.findById(saved.getId())).isEmpty();
    }
}