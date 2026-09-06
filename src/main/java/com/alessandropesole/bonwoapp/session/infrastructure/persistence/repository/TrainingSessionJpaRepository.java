package com.alessandropesole.bonwoapp.session.infrastructure.persistence.repository;

import com.alessandropesole.bonwoapp.session.domain.model.SessionStatus;
import com.alessandropesole.bonwoapp.session.infrastructure.persistence.entity.TrainingSessionJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrainingSessionJpaRepository extends JpaRepository<TrainingSessionJpaEntity, Long> {
    Page<TrainingSessionJpaEntity> findByOwnerId(Long ownerId, Pageable pageable);

    boolean existsByOwnerIdAndStatus(Long ownerId, SessionStatus status);
}
