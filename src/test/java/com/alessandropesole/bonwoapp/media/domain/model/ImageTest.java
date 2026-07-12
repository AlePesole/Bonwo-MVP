package com.alessandropesole.bonwoapp.media.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageTest {

    @Test
    void createPending_setsFieldsAndGeneratesUploadToken() {
        Image image = Image.createPending(1L, "ext-id", "http://url", 15L);

        assertThat(image.getOwnerId()).isEqualTo(1L);
        assertThat(image.getExternalId()).isEqualTo("ext-id");
        assertThat(image.getUrl()).isEqualTo("http://url");
        assertThat(image.getStatus()).isEqualTo(ImageStatus.PENDING);
        assertThat(image.getUploadToken()).isNotBlank();
        assertThat(image.getExpiresAt()).isAfter(Instant.now());
        assertThat(image.getCreatedAt()).isNotNull();
    }

    @Test
    void createPending_rejectsNullOwnerId() {
        assertThatThrownBy(() -> Image.createPending(null, "ext-id", "url", 15L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ownerId");
    }

    @Test
    void createPending_rejectsBlankExternalId() {
        assertThatThrownBy(() -> Image.createPending(1L, "  ", "url", 15L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("externalId");
    }

    @Test
    void activate_setsActiveAndClearsTokenAndExpiry() {
        Image image = Image.createPending(1L, "ext-id", "url", 15L);

        image.activate();

        assertThat(image.getStatus()).isEqualTo(ImageStatus.ACTIVE);
        assertThat(image.getUploadToken()).isNull();
        assertThat(image.getExpiresAt()).isNull();
        assertThat(image.isActive()).isTrue();
        assertThat(image.isPending()).isFalse();
    }

    @Test
    void isExpired_trueWhenExpiresAtIsInThePast() {
        Image image = Image.reconstitute(1L, 1L, "ext-id", "url", ImageStatus.PENDING,
                "token", Instant.now().minusSeconds(60), Instant.now());

        assertThat(image.isExpired()).isTrue();
    }

    @Test
    void isExpired_falseWhenExpiresAtIsInTheFuture() {
        Image image = Image.reconstitute(1L, 1L, "ext-id", "url", ImageStatus.PENDING,
                "token", Instant.now().plusSeconds(60), Instant.now());

        assertThat(image.isExpired()).isFalse();
    }

    @Test
    void isExpired_falseWhenExpiresAtIsNull() {
        Image image = Image.reconstitute(1L, 1L, "ext-id", "url", ImageStatus.ACTIVE,
                null, null, Instant.now());

        assertThat(image.isExpired()).isFalse();
    }

    @Test
    void isOwnedBy_trueForMatchingOwner() {
        Image image = Image.createPending(1L, "ext-id", "url", 15L);

        assertThat(image.isOwnedBy(1L)).isTrue();
        assertThat(image.isOwnedBy(2L)).isFalse();
        assertThat(image.isOwnedBy(null)).isFalse();
    }
}