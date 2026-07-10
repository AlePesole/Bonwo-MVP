package com.alessandropesole.bonwoapp.user.infrastructure.persistence.mapper;

import com.alessandropesole.bonwoapp.user.domain.model.User;
import com.alessandropesole.bonwoapp.user.domain.model.UserProfile;
import com.alessandropesole.bonwoapp.user.infrastructure.persistence.entity.UserJpaEntity;

public final class UserMapper {

    private UserMapper() {
    }

    public static User toDomain(UserJpaEntity e) {
        UserProfile profile = UserProfileMapper.toDomain(e);

        return User.reconstitute(
                e.getId(), e.getEmail(), e.getPasswordHash(), e.getUsername(),
                e.getRole(), e.getStatus(), profile, e.getCreatedAt()
        );
    }

    public static UserJpaEntity toEntity(User u) {
        UserJpaEntity.UserJpaEntityBuilder builder = UserJpaEntity.builder()
                .id(u.getId())
                .email(u.getEmail())
                .passwordHash(u.getPasswordHash())
                .username(u.getUsername())
                .role(u.getRole())
                .status(u.getStatus())
                .createdAt(u.getCreatedAt());

        UserProfileMapper.applyToEntityBuilder(u.getProfile(), builder);

        return builder.build();
    }
}
