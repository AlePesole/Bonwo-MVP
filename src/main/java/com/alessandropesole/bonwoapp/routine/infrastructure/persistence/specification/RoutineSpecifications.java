package com.alessandropesole.bonwoapp.routine.infrastructure.persistence.specification;

import com.alessandropesole.bonwoapp.routine.infrastructure.persistence.entity.RoutineJpaEntity;
import org.springframework.data.jpa.domain.Specification;

import java.util.Set;

public final class RoutineSpecifications {

    private RoutineSpecifications() {
    }

    public static Specification<RoutineJpaEntity> matching(Long ownerId, Set<Long> equipmentIds,
                                                            Set<Long> activityIds, Set<Long> trainingGoalIds) {
        return (root, query, cb) -> {
            query.distinct(true);

            var predicate = cb.equal(root.get("ownerId"), ownerId);

            if (equipmentIds != null && !equipmentIds.isEmpty()) {
                var equipment = root.<RoutineJpaEntity, Long>join("equipmentIds");
                predicate = cb.and(predicate, equipment.in(equipmentIds));
            }
            if (activityIds != null && !activityIds.isEmpty()) {
                var activities = root.<RoutineJpaEntity, Long>join("activityIds");
                predicate = cb.and(predicate, activities.in(activityIds));
            }
            if (trainingGoalIds != null && !trainingGoalIds.isEmpty()) {
                var trainingGoals = root.<RoutineJpaEntity, Long>join("trainingGoalIds");
                predicate = cb.and(predicate, trainingGoals.in(trainingGoalIds));
            }

            return predicate;
        };
    }
}
