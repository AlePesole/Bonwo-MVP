package com.alessandropesole.bonwoapp.user.domain.port.in;

import com.alessandropesole.bonwoapp.user.application.dto.UpdateProfileRequest;
import com.alessandropesole.bonwoapp.user.application.dto.UserProfileResponse;

public interface UserProfileUseCase {
    UserProfileResponse getMyProfile(Long userId);

    UserProfileResponse getPublicProfile(String username);

    UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request);
}
