package com.alessandropesole.bonwoapp.user.infrastructure.persistence.mapper;

import com.alessandropesole.bonwoapp.user.domain.model.RefreshToken;
import com.alessandropesole.bonwoapp.user.infrastructure.persistence.entity.RefreshTokenJpaEntity;

public final class RefreshTokenMapper {

    private RefreshTokenMapper() {
    }

    public static RefreshToken toDomain(RefreshTokenJpaEntity e) {
        return RefreshToken.reconstitute(
                e.getId(), e.getUserId(), e.getTokenId(), e.getExpiresAt(), e.isRevoked(), e.getCreatedAt()
        );
    }

    public static RefreshTokenJpaEntity toEntity(RefreshToken rt) {
        return RefreshTokenJpaEntity.builder()
                .id(rt.getId())
                .userId(rt.getUserId())
                .tokenId(rt.getTokenId())
                .expiresAt(rt.getExpiresAt())
                .revoked(rt.isRevoked())
                .createdAt(rt.getCreatedAt())
                .build();
    }
}
