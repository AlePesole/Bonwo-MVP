package com.alessandropesole.bonwoapp.routine.infrastructure.persistence.specification;

import com.alessandropesole.bonwoapp.exercise.domain.model.ActivationLevel;
import com.alessandropesole.bonwoapp.exercise.infrastructure.persistence.entity.ExerciseJpaEntity;
import com.alessandropesole.bonwoapp.exercise.infrastructure.persistence.entity.MuscleEntryEmbeddable;
import com.alessandropesole.bonwoapp.routine.infrastructure.persistence.entity.ExerciseSlotJpaEntity;
import com.alessandropesole.bonwoapp.routine.infrastructure.persistence.entity.RoutineJpaEntity;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.util.Set;

public final class RoutineSpecifications {

    private RoutineSpecifications() {
    }

    public static Specification<RoutineJpaEntity> matching(Long ownerId, Set<Long> muscleSubGroupIds,
                                                            Set<Long> equipmentIds, Set<Long> activityIds,
                                                            Set<Long> trainingGoalIds, String title) {
        return (root, query, cb) -> {
            query.distinct(true);

            var predicate = cb.and(
                    cb.equal(root.get("ownerId"), ownerId),
                    cb.isNull(root.get("trainingProgramId")));

            if (title != null && !title.isBlank()) {
                predicate = cb.and(predicate, cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase() + "%"));
            }
            if (muscleSubGroupIds != null && !muscleSubGroupIds.isEmpty()) {
                var slots = root.<RoutineJpaEntity, ExerciseSlotJpaEntity>join("slots");
                Root<ExerciseJpaEntity> exerciseRoot = query.from(ExerciseJpaEntity.class);
                var muscles = exerciseRoot.<ExerciseJpaEntity, MuscleEntryEmbeddable>join("muscles");
                predicate = cb.and(predicate,
                        cb.equal(slots.get("exerciseId"), exerciseRoot.get("id")),
                        muscles.get("subGroupId").in(muscleSubGroupIds),
                        cb.greaterThanOrEqualTo(muscles.get("activation"), ActivationLevel.PRIMARY_THRESHOLD));
            }
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
