package com.alessandropesole.bonwoapp.user.domain.model;

import com.alessandropesole.bonwoapp.user.domain.exception.InvalidProfileDataException;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserProfileTest {

    @Test
    void empty_hasAllNullFieldsAndEmptyActivityIds() {
        UserProfile profile = UserProfile.empty();

        assertThat(profile.getAvatarId()).isNull();
        assertThat(profile.getBio()).isNull();
        assertThat(profile.getActivityIds()).isEmpty();
    }

    @Test
    void of_setsAllFields() {
        UserProfile profile = UserProfile.of(1L, "bio", 30, 180, 80.0,
                new LinkedHashSet<>(Set.of(1L, 2L)));

        assertThat(profile.getAvatarId()).isEqualTo(1L);
        assertThat(profile.getBio()).isEqualTo("bio");
        assertThat(profile.getAgeYears()).isEqualTo(30);
        assertThat(profile.getHeightCm()).isEqualTo(180);
        assertThat(profile.getWeightKg()).isEqualTo(80.0);
        assertThat(profile.getActivityIds()).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void of_activityIdsAreStoredAsAnUnmodifiableSet() {
        UserProfile profile = UserProfile.of(null, null, null, null, null, Set.of(1L));

        assertThatThrownBy(() -> profile.getActivityIds().add(2L))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void of_defaultsToEmptySetWhenActivityIdsIsNull() {
        UserProfile profile = UserProfile.of(null, null, null, null, null, null);

        assertThat(profile.getActivityIds()).isEmpty();
    }

    @Test
    void of_rejectsAgeOutsideRange() {
        assertThatThrownBy(() -> UserProfile.of(null, null, 12, null, null, null))
                .isInstanceOf(InvalidProfileDataException.class)
                .hasMessageContaining("Age");
        assertThatThrownBy(() -> UserProfile.of(null, null, 121, null, null, null))
                .isInstanceOf(InvalidProfileDataException.class)
                .hasMessageContaining("Age");
    }

    @Test
    void of_rejectsHeightOutsideRange() {
        assertThatThrownBy(() -> UserProfile.of(null, null, null, 49, null, null))
                .isInstanceOf(InvalidProfileDataException.class)
                .hasMessageContaining("Height");
        assertThatThrownBy(() -> UserProfile.of(null, null, null, 301, null, null))
                .isInstanceOf(InvalidProfileDataException.class)
                .hasMessageContaining("Height");
    }

    @Test
    void of_rejectsWeightOutsideRange() {
        assertThatThrownBy(() -> UserProfile.of(null, null, null, null, 19.9, null))
                .isInstanceOf(InvalidProfileDataException.class)
                .hasMessageContaining("Weight");
        assertThatThrownBy(() -> UserProfile.of(null, null, null, null, 500.1, null))
                .isInstanceOf(InvalidProfileDataException.class)
                .hasMessageContaining("Weight");
    }

    @Test
    void of_rejectsBioLongerThan500Characters() {
        assertThatThrownBy(() -> UserProfile.of(null, "a".repeat(501), null, null, null, null))
                .isInstanceOf(InvalidProfileDataException.class)
                .hasMessageContaining("Bio");
    }

    @Test
    void withAvatarId_returnsNewInstanceWithOnlyAvatarChanged() {
        UserProfile original = UserProfile.of(1L, "bio", 30, 180, 80.0, Set.of(1L));

        UserProfile updated = original.withAvatarId(2L);

        assertThat(updated.getAvatarId()).isEqualTo(2L);
        assertThat(updated.getBio()).isEqualTo("bio");
        assertThat(updated.getActivityIds()).containsExactly(1L);
        assertThat(original.getAvatarId()).isEqualTo(1L);
    }

    @Test
    void applyUpdate_nullFieldsKeepCurrentValues() {
        UserProfile original = UserProfile.of(1L, "old bio", 30, 180, 80.0, Set.of(1L));

        UserProfile updated = original.applyUpdate(null, null, null, null, null);

        assertThat(updated.getBio()).isEqualTo("old bio");
        assertThat(updated.getAgeYears()).isEqualTo(30);
        assertThat(updated.getHeightCm()).isEqualTo(180);
        assertThat(updated.getWeightKg()).isEqualTo(80.0);
        assertThat(updated.getActivityIds()).containsExactly(1L);
    }

    @Test
    void applyUpdate_overwritesOnlyProvidedFields() {
        UserProfile original = UserProfile.of(1L, "old bio", 30, 180, 80.0, Set.of(1L));

        UserProfile updated = original.applyUpdate("new bio", 31, null, null, Set.of(2L, 3L));

        assertThat(updated.getBio()).isEqualTo("new bio");
        assertThat(updated.getAgeYears()).isEqualTo(31);
        assertThat(updated.getHeightCm()).isEqualTo(180);
        assertThat(updated.getWeightKg()).isEqualTo(80.0);
        assertThat(updated.getActivityIds()).containsExactlyInAnyOrder(2L, 3L);
        assertThat(updated.getAvatarId()).isEqualTo(1L);
    }

    @Test
    void applyUpdate_validatesMergedValues() {
        UserProfile original = UserProfile.of(1L, "bio", 30, 180, 80.0, null);

        assertThatThrownBy(() -> original.applyUpdate(null, 200, null, null, null))
                .isInstanceOf(InvalidProfileDataException.class);
    }
}
