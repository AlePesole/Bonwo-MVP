package com.alessandropesole.bonwoapp.catalog.application.service;

import com.alessandropesole.bonwoapp.catalog.application.dto.ActivityRequest;
import com.alessandropesole.bonwoapp.catalog.application.dto.ActivityResponse;
import com.alessandropesole.bonwoapp.catalog.application.exception.DuplicateNameException;
import com.alessandropesole.bonwoapp.catalog.domain.model.Activity;
import com.alessandropesole.bonwoapp.catalog.domain.port.out.ActivityRepository;
import com.alessandropesole.bonwoapp.media.application.dto.ImageResponse;
import com.alessandropesole.bonwoapp.media.application.service.MediaResolver;
import com.alessandropesole.bonwoapp.media.application.service.MediaService;
import com.alessandropesole.bonwoapp.shared.infrastructure.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityServiceTest {

    @Mock
    private ActivityRepository activityRepository;
    @Mock
    private MediaResolver mediaResolver;
    @Mock
    private MediaService mediaService;

    @InjectMocks
    private ActivityService activityService;

    private static final Long ADMIN_ID = 1L;

    @Test
    void listAll_mapsEachActivityToResponseWithResolvedIcon() {
        Activity a = Activity.reconstitute(1L, "Running", "Cardio", 10L);
        when(activityRepository.findAll()).thenReturn(List.of(a));
        when(mediaResolver.resolveImage(10L)).thenReturn(new ImageResponse(10L, "url", null));

        List<ActivityResponse> result = activityService.listAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Running");
        assertThat(result.get(0).icon().id()).isEqualTo(10L);
    }

    @Test
    void create_claimsIconAndSavesNewActivity() {
        ActivityRequest req = new ActivityRequest("Running", "Cardio", "upload-token");
        when(activityRepository.existsByName("Running")).thenReturn(false);
        when(mediaService.claimImage("upload-token", ADMIN_ID)).thenReturn(10L);
        when(activityRepository.save(any(Activity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ActivityResponse response = activityService.create(req, ADMIN_ID);

        assertThat(response.name()).isEqualTo("Running");
        verify(mediaService).claimImage("upload-token", ADMIN_ID);
        verify(activityRepository).save(any(Activity.class));
    }

    @Test
    void create_rejectsDuplicateName() {
        ActivityRequest req = new ActivityRequest("Running", "Cardio", "upload-token");
        when(activityRepository.existsByName("Running")).thenReturn(true);

        assertThatThrownBy(() -> activityService.create(req, ADMIN_ID))
                .isInstanceOf(DuplicateNameException.class);

        verifyNoInteractions(mediaService);
        verify(activityRepository, never()).save(any());
    }

    @Test
    void create_rejectsMissingIconUploadToken() {
        ActivityRequest req = new ActivityRequest("Running", "Cardio", null);
        when(activityRepository.existsByName("Running")).thenReturn(false);

        assertThatThrownBy(() -> activityService.create(req, ADMIN_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("iconUploadToken");

        verifyNoInteractions(mediaService);
    }

    @Test
    void update_replacesIconAndDeletesOldOneWhenNewTokenProvided() {
        Activity existing = Activity.reconstitute(1L, "Running", "Cardio", 10L);
        ActivityRequest req = new ActivityRequest("Sprinting", "Fast cardio", "new-token");
        when(activityRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(mediaService.claimImage("new-token", ADMIN_ID)).thenReturn(20L);
        when(activityRepository.save(any(Activity.class))).thenAnswer(inv -> inv.getArgument(0));

        ActivityResponse response = activityService.update(1L, req, ADMIN_ID);

        assertThat(response.name()).isEqualTo("Sprinting");
        verify(mediaService).deleteImage(10L);
        verify(mediaService).claimImage("new-token", ADMIN_ID);
    }

    @Test
    void update_keepsExistingIconWhenNoNewTokenProvided() {
        Activity existing = Activity.reconstitute(1L, "Running", "Cardio", 10L);
        ActivityRequest req = new ActivityRequest("Sprinting", "Fast cardio", null);
        when(activityRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(activityRepository.save(any(Activity.class))).thenAnswer(inv -> inv.getArgument(0));

        activityService.update(1L, req, ADMIN_ID);

        verify(mediaService, never()).claimImage(any(), any());
        verify(mediaService, never()).deleteImage(any());
    }

    @Test
    void update_throwsWhenActivityNotFound() {
        ActivityRequest req = new ActivityRequest("Sprinting", "Fast cardio", null);
        when(activityRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> activityService.update(1L, req, ADMIN_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_deletesIconAndActivity() {
        Activity existing = Activity.reconstitute(1L, "Running", "Cardio", 10L);
        when(activityRepository.findById(1L)).thenReturn(Optional.of(existing));

        activityService.delete(1L);

        verify(mediaService).deleteImage(10L);
        verify(activityRepository).deleteById(1L);
    }

    @Test
    void delete_throwsWhenActivityNotFound() {
        when(activityRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> activityService.delete(1L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(activityRepository, never()).deleteById(any());
    }
}