package com.alessandropesole.bonwoapp.catalog.application.service;

import com.alessandropesole.bonwoapp.catalog.application.dto.EquipmentRequest;
import com.alessandropesole.bonwoapp.catalog.application.dto.EquipmentResponse;
import com.alessandropesole.bonwoapp.catalog.application.exception.DuplicateNameException;
import com.alessandropesole.bonwoapp.catalog.application.mapper.EquipmentDtoMapper;
import com.alessandropesole.bonwoapp.catalog.domain.model.Equipment;
import com.alessandropesole.bonwoapp.catalog.domain.port.in.EquipmentUseCase;
import com.alessandropesole.bonwoapp.catalog.domain.port.out.EquipmentRepository;
import com.alessandropesole.bonwoapp.media.application.service.MediaResolver;
import com.alessandropesole.bonwoapp.media.application.service.MediaService;
import com.alessandropesole.bonwoapp.shared.infrastructure.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class EquipmentService implements EquipmentUseCase {

    private final EquipmentRepository equipmentRepository;
    private final MediaResolver mediaResolver;
    private final MediaService mediaService;

    @Override
    @Transactional(readOnly = true)
    public List<EquipmentResponse> listAll() {
        return equipmentRepository.findAll().stream()
                .map(eq -> EquipmentDtoMapper.toResponse(eq, mediaResolver.resolveImage(eq.getIconId())))
                .toList();
    }

    @Override
    public EquipmentResponse create(EquipmentRequest req, Long adminId) {
        if (equipmentRepository.existsByName(req.name()))
            throw new DuplicateNameException("Equipment", req.name());
        if (req.iconUploadToken() == null || req.iconUploadToken().isBlank())
            throw new IllegalArgumentException("iconUploadToken is required to create equipment");

        Long iconId = mediaService.claimImage(req.iconUploadToken(), adminId);
        Equipment saved = equipmentRepository.save(Equipment.create(req.name(), iconId));
        return EquipmentDtoMapper.toResponse(saved, mediaResolver.resolveImage(saved.getIconId()));
    }

    @Override
    public EquipmentResponse update(Long id, EquipmentRequest req, Long adminId) {
        Equipment e = equipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Equipment", id));

        Long newIconId = null;
        if (req.iconUploadToken() != null && !req.iconUploadToken().isBlank()) {
            newIconId = mediaService.claimImage(req.iconUploadToken(), adminId);
            mediaService.deleteImage(e.getIconId());
        }
        e.update(req.name(), newIconId);
        Equipment saved = equipmentRepository.save(e);
        return EquipmentDtoMapper.toResponse(saved, mediaResolver.resolveImage(saved.getIconId()));
    }

    @Override
    public void delete(Long id) {
        Equipment e = equipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Equipment", id));
        mediaService.deleteImage(e.getIconId());
        equipmentRepository.deleteById(id);
    }
}
