package com.alessandropesole.bonwoapp.user.application.mapper;

import com.alessandropesole.bonwoapp.catalog.application.dto.ActivityResponse;
import com.alessandropesole.bonwoapp.media.application.dto.ImageResponse;
import com.alessandropesole.bonwoapp.user.application.dto.UserProfileResponse;
import com.alessandropesole.bonwoapp.user.domain.model.User;

import java.util.List;

public final class UserProfileDtoMapper {

    private UserProfileDtoMapper() {
    }

    public static UserProfileResponse toResponse(User u,
                                                 ImageResponse avatar,
                                                 List<ActivityResponse> activityResponses) {
        return new UserProfileResponse(
                u.getId(),
                u.getUsername(),
                avatar,
                u.getProfile().getBio(),
                u.getProfile().getAgeYears(),
                u.getProfile().getHeightCm(),
                u.getProfile().getWeightKg(),
                activityResponses
        );
    }
}
