package com.alessandropesole.bonwoapp.catalog.application.service;

import com.alessandropesole.bonwoapp.catalog.application.dto.EquipmentRequest;
import com.alessandropesole.bonwoapp.catalog.application.dto.EquipmentResponse;
import com.alessandropesole.bonwoapp.catalog.application.exception.DuplicateNameException;
import com.alessandropesole.bonwoapp.catalog.domain.model.Equipment;
import com.alessandropesole.bonwoapp.catalog.domain.port.out.EquipmentRepository;
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
class EquipmentServiceTest {

    @Mock
    private EquipmentRepository equipmentRepository;
    @Mock
    private MediaResolver mediaResolver;
    @Mock
    private MediaService mediaService;

    @InjectMocks
    private EquipmentService equipmentService;

    private static final Long ADMIN_ID = 1L;

    @Test
    void listAll_mapsEachEquipmentToResponseWithResolvedIcon() {
        Equipment e = Equipment.reconstitute(1L, "Dumbbell", 10L);
        when(equipmentRepository.findAll()).thenReturn(List.of(e));
        when(mediaResolver.resolveImage(10L)).thenReturn(new ImageResponse(10L, "url", null));

        List<EquipmentResponse> result = equipmentService.listAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Dumbbell");
        assertThat(result.get(0).icon().id()).isEqualTo(10L);
    }

    @Test
    void create_claimsIconAndSavesNewEquipment() {
        EquipmentRequest req = new EquipmentRequest("Dumbbell", "upload-token");
        when(equipmentRepository.existsByName("Dumbbell")).thenReturn(false);
        when(mediaService.claimImage("upload-token", ADMIN_ID)).thenReturn(10L);
        when(equipmentRepository.save(any(Equipment.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        EquipmentResponse response = equipmentService.create(req, ADMIN_ID);

        assertThat(response.name()).isEqualTo("Dumbbell");
        verify(mediaService).claimImage("upload-token", ADMIN_ID);
        verify(equipmentRepository).save(any(Equipment.class));
    }

    @Test
    void create_rejectsDuplicateName() {
        EquipmentRequest req = new EquipmentRequest("Dumbbell", "upload-token");
        when(equipmentRepository.existsByName("Dumbbell")).thenReturn(true);

        assertThatThrownBy(() -> equipmentService.create(req, ADMIN_ID))
                .isInstanceOf(DuplicateNameException.class);

        verifyNoInteractions(mediaService);
        verify(equipmentRepository, never()).save(any());
    }

    @Test
    void create_rejectsMissingIconUploadToken() {
        EquipmentRequest req = new EquipmentRequest("Dumbbell", null);
        when(equipmentRepository.existsByName("Dumbbell")).thenReturn(false);

        assertThatThrownBy(() -> equipmentService.create(req, ADMIN_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("iconUploadToken");

        verifyNoInteractions(mediaService);
    }

    @Test
    void update_replacesIconAndDeletesOldOneWhenNewTokenProvided() {
        Equipment existing = Equipment.reconstitute(1L, "Dumbbell", 10L);
        EquipmentRequest req = new EquipmentRequest("Kettlebell", "new-token");
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(mediaService.claimImage("new-token", ADMIN_ID)).thenReturn(20L);
        when(equipmentRepository.save(any(Equipment.class))).thenAnswer(inv -> inv.getArgument(0));

        EquipmentResponse response = equipmentService.update(1L, req, ADMIN_ID);

        assertThat(response.name()).isEqualTo("Kettlebell");
        verify(mediaService).deleteImage(10L);
        verify(mediaService).claimImage("new-token", ADMIN_ID);
    }

    @Test
    void update_keepsExistingIconWhenNoNewTokenProvided() {
        Equipment existing = Equipment.reconstitute(1L, "Dumbbell", 10L);
        EquipmentRequest req = new EquipmentRequest("Kettlebell", null);
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(equipmentRepository.save(any(Equipment.class))).thenAnswer(inv -> inv.getArgument(0));

        equipmentService.update(1L, req, ADMIN_ID);

        verify(mediaService, never()).claimImage(any(), any());
        verify(mediaService, never()).deleteImage(any());
    }

    @Test
    void update_throwsWhenEquipmentNotFound() {
        EquipmentRequest req = new EquipmentRequest("Kettlebell", null);
        when(equipmentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> equipmentService.update(1L, req, ADMIN_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_deletesIconAndEquipment() {
        Equipment existing = Equipment.reconstitute(1L, "Dumbbell", 10L);
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(existing));

        equipmentService.delete(1L);

        verify(mediaService).deleteImage(10L);
        verify(equipmentRepository).deleteById(1L);
    }

    @Test
    void delete_throwsWhenEquipmentNotFound() {
        when(equipmentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> equipmentService.delete(1L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(equipmentRepository, never()).deleteById(any());
    }
}