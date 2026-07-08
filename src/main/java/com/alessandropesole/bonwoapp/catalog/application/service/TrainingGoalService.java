package com.alessandropesole.bonwoapp.catalog.application.service;

import com.alessandropesole.bonwoapp.catalog.application.dto.TrainingGoalRequest;
import com.alessandropesole.bonwoapp.catalog.application.dto.TrainingGoalResponse;
import com.alessandropesole.bonwoapp.catalog.application.exception.DuplicateNameException;
import com.alessandropesole.bonwoapp.catalog.application.mapper.TrainingGoalDtoMapper;
import com.alessandropesole.bonwoapp.catalog.domain.model.TrainingGoal;
import com.alessandropesole.bonwoapp.catalog.domain.port.in.TrainingGoalUseCase;
import com.alessandropesole.bonwoapp.catalog.domain.port.out.TrainingGoalRepository;
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
public class TrainingGoalService implements TrainingGoalUseCase {

    private final TrainingGoalRepository trainingGoalRepository;
    private final MediaResolver mediaResolver;
    private final MediaService mediaService;

    @Override
    @Transactional(readOnly = true)
    public List<TrainingGoalResponse> listAll() {
        return trainingGoalRepository.findAll().stream()
                .map(tg -> TrainingGoalDtoMapper.toResponse(tg, mediaResolver.resolveImage(tg.getIconId())))
                .toList();
    }

    @Override
    public TrainingGoalResponse create(TrainingGoalRequest req, Long adminId) {
        if (trainingGoalRepository.existsByName(req.name()))
            throw new DuplicateNameException("TrainingGoal", req.name());
        if (req.iconUploadToken() == null || req.iconUploadToken().isBlank())
            throw new IllegalArgumentException("iconUploadToken is required to create a training goal");

        Long iconId = mediaService.claimImage(req.iconUploadToken(), adminId);
        TrainingGoal saved = trainingGoalRepository.save(
                TrainingGoal.create(req.name(), req.detail(), iconId));
        return TrainingGoalDtoMapper.toResponse(saved, mediaResolver.resolveImage(saved.getIconId()));
    }

    @Override
    public TrainingGoalResponse update(Long id, TrainingGoalRequest req, Long adminId) {
        TrainingGoal t = trainingGoalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TrainingGoal", id));

        Long newIconId = null;
        if (req.iconUploadToken() != null && !req.iconUploadToken().isBlank()) {
            newIconId = mediaService.claimImage(req.iconUploadToken(), adminId);
            mediaService.deleteImage(t.getIconId());
        }
        t.update(req.name(), req.detail(), newIconId);
        TrainingGoal saved = trainingGoalRepository.save(t);
        return TrainingGoalDtoMapper.toResponse(saved, mediaResolver.resolveImage(saved.getIconId()));
    }

    @Override
    public void delete(Long id) {
        TrainingGoal t = trainingGoalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TrainingGoal", id));
        mediaService.deleteImage(t.getIconId());
        trainingGoalRepository.deleteById(id);
    }
}
