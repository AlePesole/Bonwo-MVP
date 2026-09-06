package com.alessandropesole.bonwoapp.program.infrastructure.persistence.specification;

import com.alessandropesole.bonwoapp.program.infrastructure.persistence.entity.TrainingProgramJpaEntity;
import org.springframework.data.jpa.domain.Specification;

import java.util.Set;

public final class TrainingProgramSpecifications {

    private TrainingProgramSpecifications() {
    }

    public static Specification<TrainingProgramJpaEntity> matching(Long ownerId, Set<Long> equipmentIds,
                                                                   Set<Long> activityIds, Set<Long> trainingGoalIds,
                                                                   String title) {
        return (root, query, cb) -> {
            query.distinct(true);

            var predicate = cb.equal(root.get("ownerId"), ownerId);

            if (title != null && !title.isBlank()) {
                predicate = cb.and(predicate, cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase() + "%"));
            }
            if (equipmentIds != null && !equipmentIds.isEmpty()) {
                var equipment = root.<TrainingProgramJpaEntity, Long>join("equipmentIds");
                predicate = cb.and(predicate, equipment.in(equipmentIds));
            }
            if (activityIds != null && !activityIds.isEmpty()) {
                var activities = root.<TrainingProgramJpaEntity, Long>join("activityIds");
                predicate = cb.and(predicate, activities.in(activityIds));
            }
            if (trainingGoalIds != null && !trainingGoalIds.isEmpty()) {
                var trainingGoals = root.<TrainingProgramJpaEntity, Long>join("trainingGoalIds");
                predicate = cb.and(predicate, trainingGoals.in(trainingGoalIds));
            }

            return predicate;
        };
    }
}
