package com.alessandropesole.bonwoapp.media.application.scheduler;

import com.alessandropesole.bonwoapp.media.application.service.MediaService;
import com.alessandropesole.bonwoapp.media.infrastructure.config.MediaProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpiredMediaCleanupSchedulerTest {

    @Mock
    private MediaService mediaService;
    @Mock
    private MediaProperties mediaProperties;

    @InjectMocks
    private ExpiredMediaCleanupScheduler scheduler;

    @Test
    void cleanupExpiredMedia_deletesExpiredVideosAndImages() {
        when(mediaService.deleteExpiredPendingVideos()).thenReturn(2);
        when(mediaService.deleteExpiredPendingImages()).thenReturn(1);

        scheduler.cleanupExpiredMedia();

        verify(mediaService).deleteExpiredPendingVideos();
        verify(mediaService).deleteExpiredPendingImages();
    }
}