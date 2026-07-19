package com.alessandropesole.bonwoapp.catalog.application.service;

import com.alessandropesole.bonwoapp.catalog.application.dto.CreateMuscleSubGroupRequest;
import com.alessandropesole.bonwoapp.catalog.application.dto.MuscleSubGroupResponse;
import com.alessandropesole.bonwoapp.catalog.application.dto.UpdateMuscleSubGroupRequest;
import com.alessandropesole.bonwoapp.catalog.application.exception.DuplicateNameException;
import com.alessandropesole.bonwoapp.catalog.application.mapper.MuscleSubGroupDtoMapper;
import com.alessandropesole.bonwoapp.catalog.domain.model.MuscleSubGroup;
import com.alessandropesole.bonwoapp.catalog.domain.port.in.MuscleSubGroupUseCase;
import com.alessandropesole.bonwoapp.catalog.domain.port.out.MuscleGroupRepository;
import com.alessandropesole.bonwoapp.catalog.domain.port.out.MuscleSubGroupRepository;
import com.alessandropesole.bonwoapp.media.application.service.MediaResolver;
import com.alessandropesole.bonwoapp.media.application.service.MediaService;
import com.alessandropesole.bonwoapp.shared.infrastructure.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MuscleSubGroupService implements MuscleSubGroupUseCase {

    private final MuscleSubGroupRepository muscleSubGroupRepository;
    private final MuscleGroupRepository muscleGroupRepository;
    private final MediaResolver mediaResolver;
    private final MediaService mediaService;

    @Override
    public MuscleSubGroupResponse create(CreateMuscleSubGroupRequest req, Long adminId) {
        muscleGroupRepository.findById(req.groupId())
                .orElseThrow(() -> new ResourceNotFoundException("MuscleGroup", req.groupId()));
        if (muscleSubGroupRepository.existsByGroupIdAndName(req.groupId(), req.name()))
            throw new DuplicateNameException("MuscleSubGroup", req.name());

        Long iconId = req.iconUploadToken() != null && !req.iconUploadToken().isBlank()
                ? mediaService.claimImage(req.iconUploadToken(), adminId) : null;

        MuscleSubGroup saved = muscleSubGroupRepository.save(
                MuscleSubGroup.create(req.groupId(), req.name(), req.detail(),
                        req.svgPathFront(), req.svgPathBack(), iconId));
        return MuscleSubGroupDtoMapper.toResponse(saved,
                mediaResolver.resolveImage(saved.getIconId()));
    }

    @Override
    public MuscleSubGroupResponse update(Long id, UpdateMuscleSubGroupRequest req, Long adminId) {
        MuscleSubGroup s = muscleSubGroupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MuscleSubGroup", id));

        Long oldIconId = s.getIconId();
        Long newIconId = null;
        if (req.iconUploadToken() != null && !req.iconUploadToken().isBlank()) {
            newIconId = mediaService.claimImage(req.iconUploadToken(), adminId);
        }
        s.update(req.name(), req.detail(), req.svgPathFront(), req.svgPathBack(), newIconId);
        MuscleSubGroup saved = muscleSubGroupRepository.save(s);
        if (newIconId != null) {
            mediaService.deleteImage(oldIconId);
        }
        return MuscleSubGroupDtoMapper.toResponse(saved,
                mediaResolver.resolveImage(saved.getIconId()));
    }

    @Override
    public void delete(Long id) {
        MuscleSubGroup s = muscleSubGroupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MuscleSubGroup", id));
        mediaService.deleteImage(s.getIconId());
        muscleSubGroupRepository.deleteById(id);
    }
}
