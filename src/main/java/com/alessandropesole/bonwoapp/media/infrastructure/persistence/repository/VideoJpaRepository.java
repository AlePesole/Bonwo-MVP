package com.alessandropesole.bonwoapp.media.infrastructure.persistence.repository;

import com.alessandropesole.bonwoapp.media.domain.model.VideoStatus;
import com.alessandropesole.bonwoapp.media.infrastructure.persistence.entity.VideoJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface VideoJpaRepository extends JpaRepository<VideoJpaEntity, Long> {
    Optional<VideoJpaEntity> findByUploadToken(String token);

    @Query("SELECT v FROM VideoJpaEntity v WHERE v.status = 'PENDING' AND v.expiresAt < :now")
    List<VideoJpaEntity> findAllExpiredPending(@org.springframework.data.repository.query.Param("now") Instant now);
}
