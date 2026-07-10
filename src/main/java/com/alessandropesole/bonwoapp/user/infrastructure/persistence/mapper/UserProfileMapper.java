package com.alessandropesole.bonwoapp.user.infrastructure.persistence.mapper;

import com.alessandropesole.bonwoapp.user.domain.model.UserProfile;
import com.alessandropesole.bonwoapp.user.infrastructure.persistence.entity.UserJpaEntity;

import java.util.ArrayList;
import java.util.LinkedHashSet;

public final class UserProfileMapper {

    private UserProfileMapper() {
    }

    public static UserProfile toDomain(UserJpaEntity e) {
        return UserProfile.of(
                e.getAvatarId(),
                e.getBio(),
                e.getAgeYears(),
                e.getHeightCm(),
                e.getWeightKg(),
                new LinkedHashSet<>(e.getActivityIds())
        );
    }

    public static void applyToEntityBuilder(UserProfile profile,
                                            UserJpaEntity.UserJpaEntityBuilder builder) {
        builder
                .avatarId(profile.getAvatarId())
                .bio(profile.getBio())
                .ageYears(profile.getAgeYears())
                .heightCm(profile.getHeightCm())
                .weightKg(profile.getWeightKg())
                .activityIds(new ArrayList<>(profile.getActivityIds()));
    }
}
