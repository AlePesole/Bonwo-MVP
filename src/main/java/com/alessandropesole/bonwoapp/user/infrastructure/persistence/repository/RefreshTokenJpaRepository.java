package com.alessandropesole.bonwoapp.user.infrastructure.persistence.repository;

import com.alessandropesole.bonwoapp.user.infrastructure.persistence.entity.RefreshTokenJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenJpaEntity, Long> {
    Optional<RefreshTokenJpaEntity> findByTokenId(String tokenId);
}
