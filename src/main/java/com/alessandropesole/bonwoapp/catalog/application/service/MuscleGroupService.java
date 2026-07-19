package com.alessandropesole.bonwoapp.catalog.application.service;

import com.alessandropesole.bonwoapp.catalog.application.dto.MuscleGroupRequest;
import com.alessandropesole.bonwoapp.catalog.application.dto.MuscleGroupResponse;
import com.alessandropesole.bonwoapp.catalog.application.exception.DuplicateNameException;
import com.alessandropesole.bonwoapp.catalog.application.mapper.MuscleGroupDtoMapper;
import com.alessandropesole.bonwoapp.catalog.application.mapper.MuscleSubGroupDtoMapper;
import com.alessandropesole.bonwoapp.catalog.domain.model.MuscleGroup;
import com.alessandropesole.bonwoapp.catalog.domain.model.MuscleSubGroup;
import com.alessandropesole.bonwoapp.catalog.domain.port.in.MuscleGroupUseCase;
import com.alessandropesole.bonwoapp.catalog.domain.port.out.MuscleGroupRepository;
import com.alessandropesole.bonwoapp.catalog.domain.port.out.MuscleSubGroupRepository;
import com.alessandropesole.bonwoapp.media.application.service.MediaResolver;
import com.alessandropesole.bonwoapp.media.application.service.MediaService;
import com.alessandropesole.bonwoapp.shared.infrastructure.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MuscleGroupService implements MuscleGroupUseCase {

    private final MuscleGroupRepository muscleGroupRepository;
    private final MuscleSubGroupRepository muscleSubGroupRepository;
    private final MediaResolver mediaResolver;
    private final MediaService mediaService;

    @Override
    @Transactional(readOnly = true)
    public List<MuscleGroupResponse> listAll() {
        var groups = muscleGroupRepository.findAll();
        var allSubs = muscleSubGroupRepository.findAll();

        var subsByGroup = allSubs.stream()
                .collect(Collectors.groupingBy(MuscleSubGroup::getGroupId));

        return groups.stream()
                .map(g -> MuscleGroupDtoMapper.toResponse(
                        g,
                        mediaResolver.resolveImage(g.getIconId()),
                        subsByGroup.getOrDefault(g.getId(), List.of()).stream()
                                .map(s -> MuscleSubGroupDtoMapper.toResponse(s,
                                        mediaResolver.resolveImage(s.getIconId())))
                                .toList()))
                .toList();
    }

    @Override
    public MuscleGroupResponse create(MuscleGroupRequest req, Long adminId) {
        if (muscleGroupRepository.existsByName(req.name()))
            throw new DuplicateNameException("MuscleGroup", req.name());
        if (req.iconUploadToken() == null || req.iconUploadToken().isBlank())
            throw new IllegalArgumentException("iconUploadToken is required");

        Long iconId = mediaService.claimImage(req.iconUploadToken(), adminId);
        MuscleGroup saved = muscleGroupRepository.save(MuscleGroup.create(req.name(), iconId));
        return MuscleGroupDtoMapper.toResponse(saved,
                mediaResolver.resolveImage(saved.getIconId()), List.of());
    }

    @Override
    public MuscleGroupResponse update(Long id, MuscleGroupRequest req, Long adminId) {
        MuscleGroup g = muscleGroupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MuscleGroup", id));

        Long oldIconId = g.getIconId();
        Long newIconId = null;
        if (req.iconUploadToken() != null && !req.iconUploadToken().isBlank()) {
            newIconId = mediaService.claimImage(req.iconUploadToken(), adminId);
        }
        g.update(req.name(), newIconId);
        MuscleGroup saved = muscleGroupRepository.save(g);
        if (newIconId != null) {
            mediaService.deleteImage(oldIconId);
        }

        var subResponses = muscleSubGroupRepository.findByGroupId(id).stream()
                .map(s -> MuscleSubGroupDtoMapper.toResponse(s,
                        mediaResolver.resolveImage(s.getIconId())))
                .toList();
        return MuscleGroupDtoMapper.toResponse(saved,
                mediaResolver.resolveImage(saved.getIconId()), subResponses);
    }

    @Override
    public void delete(Long id) {
        MuscleGroup g = muscleGroupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MuscleGroup", id));
        muscleSubGroupRepository.findByGroupId(id).forEach(sub -> {
            mediaService.deleteImage(sub.getIconId());
            muscleSubGroupRepository.deleteById(sub.getId());
        });
        mediaService.deleteImage(g.getIconId());
        muscleGroupRepository.deleteById(id);
    }
}
