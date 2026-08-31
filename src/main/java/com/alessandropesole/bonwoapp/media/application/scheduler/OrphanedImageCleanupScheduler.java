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
public class OrphanedImageCleanupScheduler {

    private final MediaService mediaService;
    private final MediaProperties mediaProperties;

    @Scheduled(fixedDelayString = "#{@mediaProperties.orphanSweepIntervalHours() * 60 * 60 * 1000}")
    @Transactional
    public void cleanupOrphanedImages() {
        int deleted = mediaService.deleteOrphanedImages();
        if (deleted > 0) {
            log.info("Cleaned up orphaned images — count={}", deleted);
        }
    }
}
