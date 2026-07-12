package com.alessandropesole.bonwoapp.media.domain.model;

import com.alessandropesole.bonwoapp.media.domain.exception.VideoNotReadyException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VideoTest {

    @Test
    void createPending_setsFieldsAndGeneratesUploadToken() {
        Video video = Video.createPending(1L, "ext-id", "http://url", "http://thumb", 42, 15L);

        assertThat(video.getOwnerId()).isEqualTo(1L);
        assertThat(video.getExternalId()).isEqualTo("ext-id");
        assertThat(video.getThumbnailUrl()).isEqualTo("http://thumb");
        assertThat(video.getDurationSeconds()).isEqualTo(42);
        assertThat(video.getStatus()).isEqualTo(VideoStatus.PENDING);
        assertThat(video.getUploadToken()).isNotBlank();
        assertThat(video.getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void createPending_rejectsNullOwnerId() {
        assertThatThrownBy(() -> Video.createPending(null, "ext-id", "url", "thumb", 1, 15L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ownerId");
    }

    @Test
    void createPending_rejectsBlankExternalId() {
        assertThatThrownBy(() -> Video.createPending(1L, "", "url", "thumb", 1, 15L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("externalId");
    }

    @Test
    void activate_setsActiveAndClearsTokenAndExpiry() {
        Video video = Video.createPending(1L, "ext-id", "url", "thumb", 1, 15L);

        video.activate();

        assertThat(video.getStatus()).isEqualTo(VideoStatus.ACTIVE);
        assertThat(video.getUploadToken()).isNull();
        assertThat(video.getExpiresAt()).isNull();
        assertThat(video.isActive()).isTrue();
    }

    @Test
    void markFailed_setsFailedStatus() {
        Video video = Video.createPending(1L, "ext-id", "url", "thumb", 1, 15L);

        video.markFailed();

        assertThat(video.getStatus()).isEqualTo(VideoStatus.FAILED);
    }

    @Test
    void assertActive_throwsWhenNotActive() {
        Video video = Video.reconstitute(1L, 1L, "ext-id", "url", "thumb", 1,
                VideoStatus.PENDING, "token", Instant.now().plusSeconds(60), Instant.now());

        assertThatThrownBy(video::assertActive)
                .isInstanceOf(VideoNotReadyException.class);
    }

    @Test
    void assertActive_doesNotThrowWhenActive() {
        Video video = Video.reconstitute(1L, 1L, "ext-id", "url", "thumb", 1,
                VideoStatus.ACTIVE, null, null, Instant.now());

        video.assertActive();
    }

    @Test
    void isExpired_trueWhenExpiresAtIsInThePast() {
        Video video = Video.reconstitute(1L, 1L, "ext-id", "url", "thumb", 1,
                VideoStatus.PENDING, "token", Instant.now().minusSeconds(60), Instant.now());

        assertThat(video.isExpired()).isTrue();
    }

    @Test
    void isExpired_falseWhenExpiresAtIsNull() {
        Video video = Video.reconstitute(1L, 1L, "ext-id", "url", "thumb", 1,
                VideoStatus.ACTIVE, null, null, Instant.now());

        assertThat(video.isExpired()).isFalse();
    }

    @Test
    void isOwnedBy_trueForMatchingOwner() {
        Video video = Video.createPending(1L, "ext-id", "url", "thumb", 1, 15L);

        assertThat(video.isOwnedBy(1L)).isTrue();
        assertThat(video.isOwnedBy(2L)).isFalse();
        assertThat(video.isOwnedBy(null)).isFalse();
    }
}