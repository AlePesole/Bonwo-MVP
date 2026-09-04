package com.alessandropesole.bonwoapp.exercise.infrastructure.persistence.specification.publication;

import com.alessandropesole.bonwoapp.exercise.domain.model.publication.PublicationType;
import com.alessandropesole.bonwoapp.exercise.domain.model.publication.Visibility;
import com.alessandropesole.bonwoapp.exercise.infrastructure.persistence.entity.ExerciseJpaEntity;
import com.alessandropesole.bonwoapp.exercise.infrastructure.persistence.entity.MuscleEntryEmbeddable;
import com.alessandropesole.bonwoapp.exercise.infrastructure.persistence.entity.publication.ExercisePublicationJpaEntity;
import com.alessandropesole.bonwoapp.exercise.infrastructure.persistence.entity.publication.ExercisePublicationLikeJpaEntity;
import com.alessandropesole.bonwoapp.exercise.infrastructure.persistence.entity.publication.ExercisePublicationSaveJpaEntity;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.util.Set;

public final class ExercisePublicationSpecifications {

    private ExercisePublicationSpecifications() {
    }

    /**
     * exercise_publications.exercise_id has no mapped JPA association to exercises (same convention
     * as the rest of the app — aggregates only reference each other via plain FK columns), so catalog
     * filters need a second, manually-correlated root instead of Root.join(...). Same reasoning for
     * likedByUserId/savedByUserId against the like/save join tables.
     */
    public static Specification<ExercisePublicationJpaEntity> matching(Long authorId, boolean onlyPublicVisibility,
                                                                        PublicationType type,
                                                                        Set<Long> muscleSubGroupIds, Set<Long> equipmentIds,
                                                                        Set<Long> activityIds, Set<Long> trainingGoalIds,
                                                                        String title, Long likedByUserId, Long savedByUserId) {
        return (root, query, cb) -> {
            query.distinct(true);
            Root<ExerciseJpaEntity> exerciseRoot = query.from(ExerciseJpaEntity.class);

            var predicate = cb.equal(root.get("exerciseId"), exerciseRoot.get("id"));

            if (authorId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("authorId"), authorId));
            }
            if (onlyPublicVisibility) {
                predicate = cb.and(predicate, cb.equal(root.get("visibility"), Visibility.PUBLIC));
            }
            if (type != null) {
                predicate = cb.and(predicate, cb.equal(root.get("type"), type));
            }
            if (title != null && !title.isBlank()) {
                predicate = cb.and(predicate, cb.like(cb.lower(exerciseRoot.get("title")), "%" + title.toLowerCase() + "%"));
            }
            if (likedByUserId != null) {
                Root<ExercisePublicationLikeJpaEntity> likeRoot = query.from(ExercisePublicationLikeJpaEntity.class);
                predicate = cb.and(predicate,
                        cb.equal(root.get("id"), likeRoot.get("publicationId")),
                        cb.equal(likeRoot.get("userId"), likedByUserId));
            }
            if (savedByUserId != null) {
                Root<ExercisePublicationSaveJpaEntity> saveRoot = query.from(ExercisePublicationSaveJpaEntity.class);
                predicate = cb.and(predicate,
                        cb.equal(root.get("id"), saveRoot.get("publicationId")),
                        cb.equal(saveRoot.get("userId"), savedByUserId));
            }

            if (muscleSubGroupIds != null && !muscleSubGroupIds.isEmpty()) {
                var muscles = exerciseRoot.<ExerciseJpaEntity, MuscleEntryEmbeddable>join("muscles");
                predicate = cb.and(predicate, muscles.get("subGroupId").in(muscleSubGroupIds));
            }
            if (equipmentIds != null && !equipmentIds.isEmpty()) {
                var equipment = exerciseRoot.<ExerciseJpaEntity, Long>join("equipmentIds");
                predicate = cb.and(predicate, equipment.in(equipmentIds));
            }
            if (activityIds != null && !activityIds.isEmpty()) {
                var activities = exerciseRoot.<ExerciseJpaEntity, Long>join("activityIds");
                predicate = cb.and(predicate, activities.in(activityIds));
            }
            if (trainingGoalIds != null && !trainingGoalIds.isEmpty()) {
                var trainingGoals = exerciseRoot.<ExerciseJpaEntity, Long>join("trainingGoalIds");
                predicate = cb.and(predicate, trainingGoals.in(trainingGoalIds));
            }

            return predicate;
        };
    }
}
