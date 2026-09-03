package com.alessandropesole.bonwoapp.exercise.domain.model.publication;

import com.alessandropesole.bonwoapp.shared.domain.AggregateRoot;
import lombok.Getter;

import java.time.Instant;

@Getter
public class ExercisePublication extends AggregateRoot {

    private Long id;
    private Long exerciseId;
    private Long authorId;
    private PublicationType type;
    private Visibility visibility;
    private long likesCount;
    private long savesCount;
    private long viewsCount;
    private long usesCount;
    private Instant publishedAt;

    public static ExercisePublication create(Long exerciseId, Long authorId, PublicationType type, Visibility visibility) {
        if (exerciseId == null) throw new IllegalArgumentException("exerciseId required");
        if (authorId == null) throw new IllegalArgumentException("authorId required");
        if (type == null) throw new IllegalArgumentException("type required");

        ExercisePublication p = new ExercisePublication();
        p.exerciseId = exerciseId;
        p.authorId = authorId;
        p.type = type;
        p.visibility = visibility != null ? visibility : Visibility.PUBLIC;
        p.likesCount = 0;
        p.savesCount = 0;
        p.viewsCount = 0;
        p.usesCount = 0;
        p.publishedAt = Instant.now();
        return p;
    }

    public static ExercisePublication reconstitute(Long id, Long exerciseId, Long authorId,
                                                   PublicationType type, Visibility visibility,
                                                   long likesCount, long savesCount, long viewsCount, long usesCount,
                                                   Instant publishedAt) {
        ExercisePublication p = new ExercisePublication();
        p.id = id;
        p.exerciseId = exerciseId;
        p.authorId = authorId;
        p.type = type;
        p.visibility = visibility;
        p.likesCount = likesCount;
        p.savesCount = savesCount;
        p.viewsCount = viewsCount;
        p.usesCount = usesCount;
        p.publishedAt = publishedAt;
        return p;
    }

    public void updateVisibility(Visibility visibility) {
        if (visibility != null) this.visibility = visibility;
    }

    public boolean isAuthoredBy(Long userId) {
        return authorId != null && authorId.equals(userId);
    }

    public boolean isVisibleTo(Long viewerId) {
        return switch (visibility) {
            case PUBLIC -> true;
        };
    }

    public void incrementLikes() {
        likesCount++;
    }

    public void decrementLikes() {
        if (likesCount > 0) likesCount--;
    }

    public void incrementSaves() {
        savesCount++;
    }

    public void decrementSaves() {
        if (savesCount > 0) savesCount--;
    }

    public void incrementViews() {
        viewsCount++;
    }

    public void incrementUses() {
        usesCount++;
    }
}
