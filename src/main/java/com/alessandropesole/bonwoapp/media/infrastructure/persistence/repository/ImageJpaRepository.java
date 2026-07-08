package com.alessandropesole.bonwoapp.media.infrastructure.persistence.repository;

import com.alessandropesole.bonwoapp.media.infrastructure.persistence.entity.ImageJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ImageJpaRepository extends JpaRepository<ImageJpaEntity, Long> {
    Optional<ImageJpaEntity> findByUploadToken(String token);

    @Query("SELECT i FROM ImageJpaEntity i WHERE i.status = 'PENDING' AND i.expiresAt < :now")
    List<ImageJpaEntity> findAllExpiredPending(@org.springframework.data.repository.query.Param("now") Instant now);
}
