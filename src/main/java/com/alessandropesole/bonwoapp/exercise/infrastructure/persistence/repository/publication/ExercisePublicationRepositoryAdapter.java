package com.alessandropesole.bonwoapp.exercise.infrastructure.persistence.repository.publication;

import com.alessandropesole.bonwoapp.exercise.domain.model.publication.ExercisePublication;
import com.alessandropesole.bonwoapp.exercise.domain.model.publication.PublicationType;
import com.alessandropesole.bonwoapp.exercise.domain.port.out.publication.ExercisePublicationRepository;
import com.alessandropesole.bonwoapp.exercise.infrastructure.persistence.mapper.publication.ExercisePublicationMapper;
import com.alessandropesole.bonwoapp.exercise.infrastructure.persistence.specification.publication.ExercisePublicationSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ExercisePublicationRepositoryAdapter implements ExercisePublicationRepository {

    private final ExercisePublicationJpaRepository jpa;

    @Override
    public ExercisePublication save(ExercisePublication publication) {
        return ExercisePublicationMapper.toDomain(jpa.save(ExercisePublicationMapper.toEntity(publication)));
    }

    @Override
    public Optional<ExercisePublication> findById(Long id) {
        return jpa.findById(id).map(ExercisePublicationMapper::toDomain);
    }

    @Override
    public Optional<ExercisePublication> findByExerciseId(Long exerciseId) {
        return jpa.findByExerciseId(exerciseId).map(ExercisePublicationMapper::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }

    @Override
    public Page<ExercisePublication> findFeed(PublicationType type, Set<Long> muscleSubGroupIds,
                                              Set<Long> equipmentIds, Set<Long> activityIds,
                                              Set<Long> trainingGoalIds, String title, Pageable pageable) {
        var spec = ExercisePublicationSpecifications.matching(null, true, type,
                muscleSubGroupIds, equipmentIds, activityIds, trainingGoalIds, title);
        return jpa.findAll(spec, pageable).map(ExercisePublicationMapper::toDomain);
    }

    @Override
    public Page<ExercisePublication> findByAuthor(Long authorId, PublicationType type, Set<Long> muscleSubGroupIds,
                                                  Set<Long> equipmentIds, Set<Long> activityIds,
                                                  Set<Long> trainingGoalIds, String title, Pageable pageable) {
        var spec = ExercisePublicationSpecifications.matching(authorId, false, type,
                muscleSubGroupIds, equipmentIds, activityIds, trainingGoalIds, title);
        return jpa.findAll(spec, pageable).map(ExercisePublicationMapper::toDomain);
    }
}
