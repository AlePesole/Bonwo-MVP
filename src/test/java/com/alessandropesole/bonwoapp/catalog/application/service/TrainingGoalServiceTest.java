package com.alessandropesole.bonwoapp.catalog.application.service;

import com.alessandropesole.bonwoapp.catalog.application.dto.TrainingGoalRequest;
import com.alessandropesole.bonwoapp.catalog.application.dto.TrainingGoalResponse;
import com.alessandropesole.bonwoapp.catalog.application.exception.DuplicateNameException;
import com.alessandropesole.bonwoapp.catalog.domain.model.TrainingGoal;
import com.alessandropesole.bonwoapp.catalog.domain.port.out.TrainingGoalRepository;
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
class TrainingGoalServiceTest {

    @Mock
    private TrainingGoalRepository trainingGoalRepository;
    @Mock
    private MediaResolver mediaResolver;
    @Mock
    private MediaService mediaService;

    @InjectMocks
    private TrainingGoalService trainingGoalService;

    private static final Long ADMIN_ID = 1L;

    @Test
    void listAll_mapsEachTrainingGoalToResponseWithResolvedIcon() {
        TrainingGoal g = TrainingGoal.reconstitute(1L, "Weight loss", "Lose fat", 10L);
        when(trainingGoalRepository.findAll()).thenReturn(List.of(g));
        when(mediaResolver.resolveImage(10L)).thenReturn(new ImageResponse(10L, "url", null));

        List<TrainingGoalResponse> result = trainingGoalService.listAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Weight loss");
        assertThat(result.get(0).icon().id()).isEqualTo(10L);
    }

    @Test
    void create_claimsIconAndSavesNewTrainingGoal() {
        TrainingGoalRequest req = new TrainingGoalRequest("Weight loss", "Lose fat", "upload-token");
        when(trainingGoalRepository.existsByName("Weight loss")).thenReturn(false);
        when(mediaService.claimImage("upload-token", ADMIN_ID)).thenReturn(10L);
        when(trainingGoalRepository.save(any(TrainingGoal.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TrainingGoalResponse response = trainingGoalService.create(req, ADMIN_ID);

        assertThat(response.name()).isEqualTo("Weight loss");
        verify(mediaService).claimImage("upload-token", ADMIN_ID);
        verify(trainingGoalRepository).save(any(TrainingGoal.class));
    }

    @Test
    void create_rejectsDuplicateName() {
        TrainingGoalRequest req = new TrainingGoalRequest("Weight loss", "Lose fat", "upload-token");
        when(trainingGoalRepository.existsByName("Weight loss")).thenReturn(true);

        assertThatThrownBy(() -> trainingGoalService.create(req, ADMIN_ID))
                .isInstanceOf(DuplicateNameException.class);

        verifyNoInteractions(mediaService);
        verify(trainingGoalRepository, never()).save(any());
    }

    @Test
    void create_rejectsMissingIconUploadToken() {
        TrainingGoalRequest req = new TrainingGoalRequest("Weight loss", "Lose fat", null);
        when(trainingGoalRepository.existsByName("Weight loss")).thenReturn(false);

        assertThatThrownBy(() -> trainingGoalService.create(req, ADMIN_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("iconUploadToken");

        verifyNoInteractions(mediaService);
    }

    @Test
    void update_replacesIconAndDeletesOldOneWhenNewTokenProvided() {
        TrainingGoal existing = TrainingGoal.reconstitute(1L, "Weight loss", "Lose fat", 10L);
        TrainingGoalRequest req = new TrainingGoalRequest("Muscle gain", "Build muscle", "new-token");
        when(trainingGoalRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(mediaService.claimImage("new-token", ADMIN_ID)).thenReturn(20L);
        when(trainingGoalRepository.save(any(TrainingGoal.class))).thenAnswer(inv -> inv.getArgument(0));

        TrainingGoalResponse response = trainingGoalService.update(1L, req, ADMIN_ID);

        assertThat(response.name()).isEqualTo("Muscle gain");
        verify(mediaService).deleteImage(10L);
        verify(mediaService).claimImage("new-token", ADMIN_ID);
    }

    @Test
    void update_keepsExistingIconWhenNoNewTokenProvided() {
        TrainingGoal existing = TrainingGoal.reconstitute(1L, "Weight loss", "Lose fat", 10L);
        TrainingGoalRequest req = new TrainingGoalRequest("Muscle gain", "Build muscle", null);
        when(trainingGoalRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(trainingGoalRepository.save(any(TrainingGoal.class))).thenAnswer(inv -> inv.getArgument(0));

        trainingGoalService.update(1L, req, ADMIN_ID);

        verify(mediaService, never()).claimImage(any(), any());
        verify(mediaService, never()).deleteImage(any());
    }

    @Test
    void update_throwsWhenTrainingGoalNotFound() {
        TrainingGoalRequest req = new TrainingGoalRequest("Muscle gain", "Build muscle", null);
        when(trainingGoalRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trainingGoalService.update(1L, req, ADMIN_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_deletesIconAndTrainingGoal() {
        TrainingGoal existing = TrainingGoal.reconstitute(1L, "Weight loss", "Lose fat", 10L);
        when(trainingGoalRepository.findById(1L)).thenReturn(Optional.of(existing));

        trainingGoalService.delete(1L);

        verify(mediaService).deleteImage(10L);
        verify(trainingGoalRepository).deleteById(1L);
    }

    @Test
    void delete_throwsWhenTrainingGoalNotFound() {
        when(trainingGoalRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trainingGoalService.delete(1L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(trainingGoalRepository, never()).deleteById(any());
    }
}