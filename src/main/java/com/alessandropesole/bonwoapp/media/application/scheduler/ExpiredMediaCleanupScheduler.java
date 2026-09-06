package com.alessandropesole.bonwoapp.media.application.scheduler;

import com.alessandropesole.bonwoapp.media.application.service.MediaService;
import com.alessandropesole.bonwoapp.media.infrastructure.config.MediaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExpiredMediaCleanupScheduler {

    private final MediaService mediaService;
    private final MediaProperties mediaProperties;

    @Scheduled(fixedDelayString = "#{@mediaProperties.pendingTtlMinutes() * 60 * 1000}")
    @Transactional
    public void cleanupExpiredMedia() {
        int videos = mediaService.deleteExpiredPendingVideos();
        int images = mediaService.deleteExpiredPendingImages();
        if (videos > 0 || images > 0) {
            log.info("Cleaned up expired pending media — videos={} images={}", videos, images);
        }
    }
}
