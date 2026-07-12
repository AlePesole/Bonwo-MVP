package com.alessandropesole.bonwoapp.media.infrastructure.persistence.repository;

import com.alessandropesole.bonwoapp.media.domain.model.Video;
import com.alessandropesole.bonwoapp.media.domain.model.VideoStatus;
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
@Import(VideoRepositoryAdapter.class)
class VideoRepositoryAdapterIT extends AbstractIntegrationTest {

    @Autowired
    private VideoRepositoryAdapter videoRepository;

    @Test
    void save_persistsAndReloadsVideo() {
        Video saved = videoRepository.save(
                Video.createPending(1L, "ext-id", "url", "thumb", 30, 15L));

        assertThat(saved.getId()).isNotNull();

        Video reloaded = videoRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getExternalId()).isEqualTo("ext-id");
        assertThat(reloaded.getStatus()).isEqualTo(VideoStatus.PENDING);
        assertThat(reloaded.getUploadToken()).isEqualTo(saved.getUploadToken());
    }

    @Test
    void findByUploadToken_returnsMatchingVideo() {
        Video saved = videoRepository.save(
                Video.createPending(1L, "ext-id", "url", "thumb", 30, 15L));

        Video found = videoRepository.findByUploadToken(saved.getUploadToken()).orElseThrow();

        assertThat(found.getId()).isEqualTo(saved.getId());
    }

    @Test
    void findByUploadToken_returnsEmptyWhenNotFound() {
        assertThat(videoRepository.findByUploadToken("unknown")).isEmpty();
    }

    @Test
    void findAllExpiredPending_onlyReturnsPendingVideosPastExpiry() {
        Video expiredPending = Video.reconstitute(null, 1L, "expired-pending", "url", "thumb", 30,
                VideoStatus.PENDING, "token-1", Instant.now().minusSeconds(60), Instant.now());
        Video futurePending = Video.reconstitute(null, 1L, "future-pending", "url", "thumb", 30,
                VideoStatus.PENDING, "token-2", Instant.now().plusSeconds(3600), Instant.now());
        Video expiredButFailed = Video.reconstitute(null, 1L, "expired-failed", "url", "thumb", 30,
                VideoStatus.FAILED, null, null, Instant.now());

        Video savedExpiredPending = videoRepository.save(expiredPending);
        videoRepository.save(futurePending);
        videoRepository.save(expiredButFailed);

        var result = videoRepository.findAllExpiredPending();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(savedExpiredPending.getId());
    }

    @Test
    void deleteById_removesVideo() {
        Video saved = videoRepository.save(
                Video.createPending(1L, "ext-id", "url", "thumb", 30, 15L));

        videoRepository.deleteById(saved.getId());

        assertThat(videoRepository.findById(saved.getId())).isEmpty();
    }
}