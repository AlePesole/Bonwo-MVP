package com.alessandropesole.bonwoapp.session.infrastructure.persistence.repository;

import com.alessandropesole.bonwoapp.session.domain.exception.SessionAlreadyInProgressException;
import com.alessandropesole.bonwoapp.session.domain.model.SessionStatus;
import com.alessandropesole.bonwoapp.session.domain.model.TrainingSession;
import com.alessandropesole.bonwoapp.session.domain.port.out.TrainingSessionRepository;
import com.alessandropesole.bonwoapp.session.infrastructure.persistence.mapper.TrainingSessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TrainingSessionRepositoryAdapter implements TrainingSessionRepository {

    private static final String IN_PROGRESS_UNIQUE_INDEX = "uq_training_sessions_owner_in_progress";

    private final TrainingSessionJpaRepository jpa;

    /** The app-level pre-check in TrainingSessionService.start() can race two concurrent
     *  requests; the partial unique index closes it at the DB level. Translate that
     *  violation into the same domain exception the pre-check throws. */
    @Override
    public TrainingSession save(TrainingSession session) {
        try {
            return TrainingSessionMapper.toDomain(jpa.save(TrainingSessionMapper.toEntity(session)));
        } catch (DataIntegrityViolationException ex) {
            if (isInProgressUniqueViolation(ex)) {
                throw new SessionAlreadyInProgressException(
                        "You already have a training session in progress. Complete or delete it before starting a new one.");
            }
            throw ex;
        }
    }

    private static boolean isInProgressUniqueViolation(DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause().getMessage();
        return message != null && message.contains(IN_PROGRESS_UNIQUE_INDEX);
    }

    @Override
    public Optional<TrainingSession> findById(Long id) {
        return jpa.findById(id).map(TrainingSessionMapper::toDomain);
    }

    @Override
    public Page<TrainingSession> findByOwner(Long ownerId, Pageable pageable) {
        return jpa.findByOwnerId(ownerId, pageable).map(TrainingSessionMapper::toDomain);
    }

    @Override
    public boolean existsByOwnerAndStatus(Long ownerId, SessionStatus status) {
        return jpa.existsByOwnerIdAndStatus(ownerId, status);
    }

    @Override
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }
}
