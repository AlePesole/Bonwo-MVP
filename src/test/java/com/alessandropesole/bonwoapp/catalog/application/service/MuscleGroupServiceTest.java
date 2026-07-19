package com.alessandropesole.bonwoapp.catalog.application.service;

import com.alessandropesole.bonwoapp.catalog.application.dto.MuscleGroupRequest;
import com.alessandropesole.bonwoapp.catalog.application.dto.MuscleGroupResponse;
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
class MuscleGroupServiceTest {

    @Mock
    private MuscleGroupRepository muscleGroupRepository;
    @Mock
    private MuscleSubGroupRepository muscleSubGroupRepository;
    @Mock
    private MediaResolver mediaResolver;
    @Mock
    private MediaService mediaService;

    @InjectMocks
    private MuscleGroupService muscleGroupService;

    private static final Long ADMIN_ID = 1L;

    @Test
    void listAll_nestsSubGroupsUnderTheirOwningGroup() {
        MuscleGroup chest = MuscleGroup.reconstitute(1L, "Chest", 10L);
        MuscleGroup back = MuscleGroup.reconstitute(2L, "Back", 20L);
        MuscleSubGroup upperChest = MuscleSubGroup.reconstitute(11L, 1L, "Upper Chest", null, "p1", null, null);
        when(muscleGroupRepository.findAll()).thenReturn(List.of(chest, back));
        when(muscleSubGroupRepository.findAll()).thenReturn(List.of(upperChest));
        when(mediaResolver.resolveImage(any())).thenReturn(null);

        List<MuscleGroupResponse> result = muscleGroupService.listAll();

        assertThat(result).hasSize(2);
        MuscleGroupResponse chestResponse = result.stream()
                .filter(r -> r.id().equals(1L)).findFirst().orElseThrow();
        MuscleGroupResponse backResponse = result.stream()
                .filter(r -> r.id().equals(2L)).findFirst().orElseThrow();
        assertThat(chestResponse.subGroups()).extracting("id").containsExactly(11L);
        assertThat(backResponse.subGroups()).isEmpty();
    }

    @Test
    void create_claimsIconAndSavesNewGroup() {
        MuscleGroupRequest req = new MuscleGroupRequest("Chest", "upload-token");
        when(muscleGroupRepository.existsByName("Chest")).thenReturn(false);
        when(mediaService.claimImage("upload-token", ADMIN_ID)).thenReturn(10L);
        when(muscleGroupRepository.save(any(MuscleGroup.class))).thenAnswer(inv -> inv.getArgument(0));

        MuscleGroupResponse response = muscleGroupService.create(req, ADMIN_ID);

        assertThat(response.name()).isEqualTo("Chest");
        assertThat(response.subGroups()).isEmpty();
        verify(mediaService).claimImage("upload-token", ADMIN_ID);
    }

    @Test
    void create_rejectsDuplicateName() {
        MuscleGroupRequest req = new MuscleGroupRequest("Chest", "upload-token");
        when(muscleGroupRepository.existsByName("Chest")).thenReturn(true);

        assertThatThrownBy(() -> muscleGroupService.create(req, ADMIN_ID))
                .isInstanceOf(DuplicateNameException.class);

        verifyNoInteractions(mediaService);
        verify(muscleGroupRepository, never()).save(any());
    }

    @Test
    void update_throwsWhenGroupNotFound() {
        MuscleGroupRequest req = new MuscleGroupRequest("Pecs", null);
        when(muscleGroupRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> muscleGroupService.update(1L, req, ADMIN_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_throwsWhenGroupNotFound() {
        when(muscleGroupRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> muscleGroupService.delete(1L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(muscleGroupRepository, never()).deleteById(any());
        verifyNoInteractions(mediaService);
    }
}