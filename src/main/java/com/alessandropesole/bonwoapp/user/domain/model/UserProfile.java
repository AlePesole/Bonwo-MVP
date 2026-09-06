package com.alessandropesole.bonwoapp.user.domain.model;

import com.alessandropesole.bonwoapp.user.domain.exception.InvalidProfileDataException;
import lombok.Getter;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
public final class UserProfile {

    private final Long avatarId;
    private final String bio;
    private final Integer ageYears;
    private final Integer heightCm;
    private final Double weightKg;
    private final Set<Long> activityIds;

    private UserProfile(Long avatarId, String bio,
                        Integer ageYears, Integer heightCm, Double weightKg,
                        Set<Long> activityIds) {
        this.avatarId = avatarId;
        this.bio = bio;
        this.ageYears = ageYears;
        this.heightCm = heightCm;
        this.weightKg = weightKg;
        this.activityIds = Collections.unmodifiableSet(new LinkedHashSet<>(activityIds));
    }

    public static UserProfile empty() {
        return new UserProfile(null, null, null, null, null, Set.of());
    }

    public static UserProfile of(Long avatarId, String bio,
                                 Integer ageYears, Integer heightCm, Double weightKg,
                                 Set<Long> activityIds) {
        validate(ageYears, heightCm, weightKg, bio);
        return new UserProfile(avatarId, bio, ageYears, heightCm, weightKg,
                activityIds != null ? activityIds : Set.of());
    }

    public UserProfile withAvatarId(Long id) {
        return new UserProfile(id, bio, ageYears, heightCm, weightKg, activityIds);
    }

    public UserProfile applyUpdate(String bio, Integer ageYears,
                                   Integer heightCm, Double weightKg,
                                   Set<Long> activityIds) {
        String newBio = bio != null ? bio : this.bio;
        Integer newAge = ageYears != null ? ageYears : this.ageYears;
        Integer newHeight = heightCm != null ? heightCm : this.heightCm;
        Double newWeight = weightKg != null ? weightKg : this.weightKg;
        Set<Long> newActs = activityIds != null ? activityIds : this.activityIds;
        validate(newAge, newHeight, newWeight, newBio);
        return new UserProfile(this.avatarId, newBio, newAge, newHeight, newWeight, newActs);
    }

    private static void validate(Integer age, Integer height, Double weight, String bio) {
        if (age != null && (age < 13 || age > 120))
            throw new InvalidProfileDataException("Age must be between 13 and 120");
        if (height != null && (height < 50 || height > 300))
            throw new InvalidProfileDataException("Height must be between 50 and 300 cm");
        if (weight != null && (weight < 20 || weight > 500))
            throw new InvalidProfileDataException("Weight must be between 20 and 500 kg");
        if (bio != null && bio.length() > 500)
            throw new InvalidProfileDataException("Bio cannot exceed 500 characters");
    }
}
