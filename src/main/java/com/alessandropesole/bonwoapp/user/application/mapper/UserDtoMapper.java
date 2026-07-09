package com.alessandropesole.bonwoapp.user.application.mapper;

import com.alessandropesole.bonwoapp.user.application.dto.UserResponse;
import com.alessandropesole.bonwoapp.user.domain.model.User;

public final class UserDtoMapper {

    private UserDtoMapper() {
    }

    public static UserResponse toResponse(User u) {
        return new UserResponse(
                u.getId(),
                u.getEmail(),
                u.getUsername(),
                u.getRole(),
                u.getStatus(),
                u.getCreatedAt()
        );
    }
}
