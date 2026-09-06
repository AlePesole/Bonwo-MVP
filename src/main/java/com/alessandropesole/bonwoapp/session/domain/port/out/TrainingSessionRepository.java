package com.alessandropesole.bonwoapp.session.domain.port.out;

import com.alessandropesole.bonwoapp.session.domain.model.SessionStatus;
import com.alessandropesole.bonwoapp.session.domain.model.TrainingSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface TrainingSessionRepository {
    TrainingSession save(TrainingSession session);

    Optional<TrainingSession> findById(Long id);

    Page<TrainingSession> findByOwner(Long ownerId, Pageable pageable);

    boolean existsByOwnerAndStatus(Long ownerId, SessionStatus status);

    void deleteById(Long id);
}
