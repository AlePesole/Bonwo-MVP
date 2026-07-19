package com.alessandropesole.bonwoapp.catalog.application.service;

import com.alessandropesole.bonwoapp.catalog.application.dto.CreateMuscleSubGroupRequest;
import com.alessandropesole.bonwoapp.catalog.application.dto.MuscleSubGroupResponse;
import com.alessandropesole.bonwoapp.catalog.application.dto.UpdateMuscleSubGroupRequest;
import com.alessandropesole.bonwoapp.catalog.application.exception.DuplicateNameException;
import com.alessandropesole.bonwoapp.catalog.domain.model.MuscleGroup;
import com.alessandropesole.bonwoapp.catalog.domain.model.MuscleSubGroup;
import com.alessandropesole.bonwoapp.catalog.domain.port.out.MuscleGroupRepository;
import com.alessandropesole.bonwoapp.catalog.domain.port.out.MuscleSubGroupRepository;
import com.alessandropesole.bonwoapp.media.application.service.MediaResolver;
import com.alessandropesole.bonwoapp.media.application.service.MediaService;
import com.alessandropesole.bonwoapp.shared.infrastructure.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MuscleSubGroupServiceTest {

    @Mock
    private MuscleSubGroupRepository muscleSubGroupRepository;
    @Mock
    private MuscleGroupRepository muscleGroupRepository;
    @Mock
    private MediaResolver mediaResolver;
    @Mock
    private MediaService mediaService;

    @InjectMocks
    private MuscleSubGroupService muscleSubGroupService;

    private static final Long ADMIN_ID = 1L;

    @Test
    void create_claimsIconAndSavesNewSubGroupVisibleFromBothSides() {
        CreateMuscleSubGroupRequest req = new CreateMuscleSubGroupRequest(
                1L, "Upper Chest", "detail", "front-1", "back-1", "upload-token");
        when(muscleGroupRepository.findById(1L)).thenReturn(Optional.of(MuscleGroup.reconstitute(1L, "Chest", 10L)));
        when(muscleSubGroupRepository.existsByGroupIdAndName(1L, "Upper Chest")).thenReturn(false);
        when(mediaService.claimImage("upload-token", ADMIN_ID)).thenReturn(20L);
        when(muscleSubGroupRepository.save(any(MuscleSubGroup.class))).thenAnswer(inv -> inv.getArgument(0));

        MuscleSubGroupResponse response = muscleSubGroupService.create(req, ADMIN_ID);

        assertThat(response.name()).isEqualTo("Upper Chest");
        assertThat(response.groupId()).isEqualTo(1L);
        assertThat(response.svgPathFront()).isEqualTo("front-1");
        assertThat(response.svgPathBack()).isEqualTo("back-1");
        verify(mediaService).claimImage("upload-token", ADMIN_ID);
    }

    @Test
    void create_allowsSubGroupVisibleFromOnlyOneSide() {
        CreateMuscleSubGroupRequest req = new CreateMuscleSubGroupRequest(
                1L, "Lats", null, null, "back-1", null);
        when(muscleGroupRepository.findById(1L)).thenReturn(Optional.of(MuscleGroup.reconstitute(1L, "Back", 10L)));
        when(muscleSubGroupRepository.existsByGroupIdAndName(1L, "Lats")).thenReturn(false);
        when(muscleSubGroupRepository.save(any(MuscleSubGroup.class))).thenAnswer(inv -> inv.getArgument(0));

        MuscleSubGroupResponse response = muscleSubGroupService.create(req, ADMIN_ID);

        assertThat(response.svgPathFront()).isNull();
        assertThat(response.svgPathBack()).isEqualTo("back-1");
    }

    @Test
    void create_rejectsWhenGroupDoesNotExist() {
        CreateMuscleSubGroupRequest req = new CreateMuscleSubGroupRequest(
                99L, "Upper Chest", null, "front-1", null, null);
        when(muscleGroupRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> muscleSubGroupService.create(req, ADMIN_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(mediaService);
        verify(muscleSubGroupRepository, never()).save(any());
    }

    @Test
    void create_rejectsDuplicateNameWithinSameGroup() {
        CreateMuscleSubGroupRequest req = new CreateMuscleSubGroupRequest(
                1L, "Upper Chest", null, "front-1", null, null);
        when(muscleGroupRepository.findById(1L)).thenReturn(Optional.of(MuscleGroup.reconstitute(1L, "Chest", 10L)));
        when(muscleSubGroupRepository.existsByGroupIdAndName(1L, "Upper Chest")).thenReturn(true);

        assertThatThrownBy(() -> muscleSubGroupService.create(req, ADMIN_ID))
                .isInstanceOf(DuplicateNameException.class);

        verifyNoInteractions(mediaService);
        verify(muscleSubGroupRepository, never()).save(any());
    }

    @Test
    void create_rejectsWhenNeitherSvgPathIsProvided() {
        CreateMuscleSubGroupRequest req = new CreateMuscleSubGroupRequest(
                1L, "Upper Chest", null, null, null, null);
        when(muscleGroupRepository.findById(1L)).thenReturn(Optional.of(MuscleGroup.reconstitute(1L, "Chest", 10L)));
        when(muscleSubGroupRepository.existsByGroupIdAndName(1L, "Upper Chest")).thenReturn(false);

        assertThatThrownBy(() -> muscleSubGroupService.create(req, ADMIN_ID))
                .isInstanceOf(com.alessandropesole.bonwoapp.catalog.domain.exception.InvalidCatalogNameException.class);

        verify(muscleSubGroupRepository, never()).save(any());
    }

    @Test
    void update_canAddBackViewToAFrontOnlySubGroup() {
        MuscleSubGroup existing = MuscleSubGroup.reconstitute(1L, 1L, "Upper Chest", null, "front-1", null, 10L);
        UpdateMuscleSubGroupRequest req = new UpdateMuscleSubGroupRequest(null, null, null, "back-1", null);
        when(muscleSubGroupRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(muscleSubGroupRepository.save(any(MuscleSubGroup.class))).thenAnswer(inv -> inv.getArgument(0));

        MuscleSubGroupResponse response = muscleSubGroupService.update(1L, req, ADMIN_ID);

        assertThat(response.svgPathFront()).isEqualTo("front-1");
        assertThat(response.svgPathBack()).isEqualTo("back-1");
    }

    @Test
    void update_throwsWhenSubGroupNotFound() {
        UpdateMuscleSubGroupRequest req = new UpdateMuscleSubGroupRequest("Mid Chest", null, null, null, null);
        when(muscleSubGroupRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> muscleSubGroupService.update(1L, req, ADMIN_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_throwsWhenSubGroupNotFound() {
        when(muscleSubGroupRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> muscleSubGroupService.delete(1L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(muscleSubGroupRepository, never()).deleteById(any());
        verifyNoInteractions(mediaService);
    }
}
