package com.alessandropesole.bonwoapp.media.infrastructure.persistence.repository;

import com.alessandropesole.bonwoapp.media.infrastructure.persistence.entity.ImageJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ImageJpaRepository extends JpaRepository<ImageJpaEntity, Long> {
    Optional<ImageJpaEntity> findByUploadToken(String token);

    @Query("SELECT i FROM ImageJpaEntity i WHERE i.status = 'PENDING' AND i.expiresAt < :now")
    List<ImageJpaEntity> findAllExpiredPending(@Param("now") Instant now);

    @Query(value = """
            SELECT i.* FROM images i
            WHERE i.status = 'ACTIVE'
              AND i.created_at < :createdBefore
              AND NOT EXISTS (SELECT 1 FROM exercises e WHERE e.thumbnail_id = i.id)
              AND NOT EXISTS (SELECT 1 FROM routines r WHERE r.thumbnail_id = i.id)
              AND NOT EXISTS (SELECT 1 FROM training_programs tp WHERE tp.thumbnail_id = i.id)
              AND NOT EXISTS (SELECT 1 FROM users u WHERE u.avatar_id = i.id)
              AND NOT EXISTS (SELECT 1 FROM equipment eq WHERE eq.icon_id = i.id)
              AND NOT EXISTS (SELECT 1 FROM activities a WHERE a.icon_id = i.id)
              AND NOT EXISTS (SELECT 1 FROM training_goals tg WHERE tg.icon_id = i.id)
              AND NOT EXISTS (SELECT 1 FROM muscle_groups mg WHERE mg.icon_id = i.id)
              AND NOT EXISTS (SELECT 1 FROM muscle_sub_groups msg WHERE msg.icon_id = i.id)
            """, nativeQuery = true)
    List<ImageJpaEntity> findAllOrphaned(@Param("createdBefore") Instant createdBefore);
}
