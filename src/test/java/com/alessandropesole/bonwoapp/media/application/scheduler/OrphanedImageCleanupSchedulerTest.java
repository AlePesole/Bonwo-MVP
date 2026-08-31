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
class OrphanedImageCleanupSchedulerTest {

    @Mock
    private MediaService mediaService;
    @Mock
    private MediaProperties mediaProperties;

    @InjectMocks
    private OrphanedImageCleanupScheduler scheduler;

    @Test
    void cleanupOrphanedImages_deletesOrphanedImages() {
        when(mediaService.deleteOrphanedImages()).thenReturn(3);

        scheduler.cleanupOrphanedImages();

        verify(mediaService).deleteOrphanedImages();
    }
}
