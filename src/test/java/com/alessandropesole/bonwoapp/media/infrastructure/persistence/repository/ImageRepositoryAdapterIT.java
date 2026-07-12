package com.alessandropesole.bonwoapp.media.infrastructure.persistence.repository;

import com.alessandropesole.bonwoapp.media.domain.model.Image;
import com.alessandropesole.bonwoapp.media.domain.model.ImageStatus;
import com.alessandropesole.bonwoapp.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ImageRepositoryAdapter.class)
class ImageRepositoryAdapterIT extends AbstractIntegrationTest {

    @Autowired
    private ImageRepositoryAdapter imageRepository;

    @Test
    void save_persistsAndReloadsImage() {
        Image saved = imageRepository.save(Image.createPending(1L, "ext-id", "url", 15L));

        assertThat(saved.getId()).isNotNull();

        Image reloaded = imageRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getExternalId()).isEqualTo("ext-id");
        assertThat(reloaded.getStatus()).isEqualTo(ImageStatus.PENDING);
        assertThat(reloaded.getUploadToken()).isEqualTo(saved.getUploadToken());
    }

    @Test
    void findByUploadToken_returnsMatchingImage() {
        Image saved = imageRepository.save(Image.createPending(1L, "ext-id", "url", 15L));

        Image found = imageRepository.findByUploadToken(saved.getUploadToken()).orElseThrow();

        assertThat(found.getId()).isEqualTo(saved.getId());
    }

    @Test
    void findByUploadToken_returnsEmptyWhenNotFound() {
        assertThat(imageRepository.findByUploadToken("unknown")).isEmpty();
    }

    @Test
    void findAllExpiredPending_onlyReturnsPendingImagesPastExpiry() {
        Image expiredPending = Image.reconstitute(null, 1L, "expired-pending", "url",
                ImageStatus.PENDING, "token-1", Instant.now().minusSeconds(60), Instant.now());
        Image futurePending = Image.reconstitute(null, 1L, "future-pending", "url",
                ImageStatus.PENDING, "token-2", Instant.now().plusSeconds(3600), Instant.now());
        Image expiredButActive = Image.reconstitute(null, 1L, "expired-active", "url",
                ImageStatus.ACTIVE, null, null, Instant.now());

        Image savedExpiredPending = imageRepository.save(expiredPending);
        imageRepository.save(futurePending);
        imageRepository.save(expiredButActive);

        var result = imageRepository.findAllExpiredPending();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(savedExpiredPending.getId());
    }

    @Test
    void deleteById_removesImage() {
        Image saved = imageRepository.save(Image.createPending(1L, "ext-id", "url", 15L));

        imageRepository.deleteById(saved.getId());

        assertThat(imageRepository.findById(saved.getId())).isEmpty();
    }
}