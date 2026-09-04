package com.alessandropesole.bonwoapp.exercise.domain.model.publication;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExercisePublicationTest {

    @Test
    void create_defaultsCountersToZeroAndVisibilityToPublic() {
        ExercisePublication publication = ExercisePublication.create(1L, 2L, PublicationType.COMMUNITY, null);

        assertThat(publication.getExerciseId()).isEqualTo(1L);
        assertThat(publication.getAuthorId()).isEqualTo(2L);
        assertThat(publication.getType()).isEqualTo(PublicationType.COMMUNITY);
        assertThat(publication.getVisibility()).isEqualTo(Visibility.PUBLIC);
        assertThat(publication.getLikesCount()).isZero();
        assertThat(publication.getSavesCount()).isZero();
        assertThat(publication.getViewsCount()).isZero();
        assertThat(publication.getUsesCount()).isZero();
        assertThat(publication.getPublishedAt()).isNotNull();
        assertThat(publication.getId()).isNull();
    }

    @Test
    void create_rejectsMissingExerciseId() {
        assertThatThrownBy(() -> ExercisePublication.create(null, 2L, PublicationType.COMMUNITY, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exerciseId");
    }

    @Test
    void create_rejectsMissingAuthorId() {
        assertThatThrownBy(() -> ExercisePublication.create(1L, null, PublicationType.COMMUNITY, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("authorId");
    }

    @Test
    void create_rejectsMissingType() {
        assertThatThrownBy(() -> ExercisePublication.create(1L, 2L, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("type");
    }

    @Test
    void incrementAndDecrementLikes_neverGoNegative() {
        ExercisePublication publication = ExercisePublication.create(1L, 2L, PublicationType.COMMUNITY, null);

        publication.decrementLikes();
        assertThat(publication.getLikesCount()).isZero();

        publication.incrementLikes();
        publication.incrementLikes();
        publication.decrementLikes();
        assertThat(publication.getLikesCount()).isEqualTo(1);
    }

    @Test
    void incrementAndDecrementSaves_neverGoNegative() {
        ExercisePublication publication = ExercisePublication.create(1L, 2L, PublicationType.COMMUNITY, null);

        publication.decrementSaves();
        assertThat(publication.getSavesCount()).isZero();

        publication.incrementSaves();
        assertThat(publication.getSavesCount()).isEqualTo(1);
    }

    @Test
    void incrementViewsAndUses_onlyGoUp() {
        ExercisePublication publication = ExercisePublication.create(1L, 2L, PublicationType.COMMUNITY, null);

        publication.incrementViews();
        publication.incrementViews();
        publication.incrementUses();

        assertThat(publication.getViewsCount()).isEqualTo(2);
        assertThat(publication.getUsesCount()).isEqualTo(1);
    }

    @Test
    void isAuthoredBy_trueOnlyForMatchingAuthorId() {
        ExercisePublication publication = ExercisePublication.create(1L, 2L, PublicationType.COMMUNITY, null);

        assertThat(publication.isAuthoredBy(2L)).isTrue();
        assertThat(publication.isAuthoredBy(99L)).isFalse();
    }

    @Test
    void isVisibleTo_trueForPublicVisibilityRegardlessOfViewer() {
        ExercisePublication publication = ExercisePublication.create(1L, 2L, PublicationType.COMMUNITY, Visibility.PUBLIC);

        assertThat(publication.isVisibleTo(2L)).isTrue();
        assertThat(publication.isVisibleTo(999L)).isTrue();
        assertThat(publication.isVisibleTo(null)).isTrue();
    }

    @Test
    void reconstitute_setsAllFieldsIncludingId() {
        ExercisePublication publication = ExercisePublication.reconstitute(
                10L, 1L, 2L, PublicationType.OFFICIAL, Visibility.PUBLIC,
                5, 3, 100, 7, null);

        assertThat(publication.getId()).isEqualTo(10L);
        assertThat(publication.getType()).isEqualTo(PublicationType.OFFICIAL);
        assertThat(publication.getLikesCount()).isEqualTo(5);
        assertThat(publication.getSavesCount()).isEqualTo(3);
        assertThat(publication.getViewsCount()).isEqualTo(100);
        assertThat(publication.getUsesCount()).isEqualTo(7);
    }
}
