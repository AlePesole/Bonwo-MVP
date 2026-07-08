package com.alessandropesole.bonwoapp.catalog.application.service;

import com.alessandropesole.bonwoapp.catalog.application.dto.ActivityRequest;
import com.alessandropesole.bonwoapp.catalog.application.dto.ActivityResponse;
import com.alessandropesole.bonwoapp.catalog.application.exception.DuplicateNameException;
import com.alessandropesole.bonwoapp.catalog.application.mapper.ActivityDtoMapper;
import com.alessandropesole.bonwoapp.catalog.domain.model.Activity;
import com.alessandropesole.bonwoapp.catalog.domain.port.in.ActivityUseCase;
import com.alessandropesole.bonwoapp.catalog.domain.port.out.ActivityRepository;
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
public class ActivityService implements ActivityUseCase {

    private final ActivityRepository activityRepository;
    private final MediaResolver mediaResolver;
    private final MediaService mediaService;

    @Override
    @Transactional(readOnly = true)
    public List<ActivityResponse> listAll() {
        return activityRepository.findAll().stream()
                .map(act -> ActivityDtoMapper.toResponse(act, mediaResolver.resolveImage(act.getIconId())))
                .toList();
    }

    @Override
    public ActivityResponse create(ActivityRequest req, Long adminId) {
        if (activityRepository.existsByName(req.name()))
            throw new DuplicateNameException("Activity", req.name());
        if (req.iconUploadToken() == null || req.iconUploadToken().isBlank())
            throw new IllegalArgumentException("iconUploadToken is required to create an activity");

        Long iconId = mediaService.claimImage(req.iconUploadToken(), adminId);
        Activity saved = activityRepository.save(Activity.create(req.name(), req.detail(), iconId));
        return ActivityDtoMapper.toResponse(saved, mediaResolver.resolveImage(saved.getIconId()));
    }

    @Override
    public ActivityResponse update(Long id, ActivityRequest req, Long adminId) {
        Activity a = activityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Activity", id));

        Long newIconId = null;
        if (req.iconUploadToken() != null && !req.iconUploadToken().isBlank()) {
            newIconId = mediaService.claimImage(req.iconUploadToken(), adminId);
            mediaService.deleteImage(a.getIconId());
        }
        a.update(req.name(), req.detail(), newIconId);
        Activity saved = activityRepository.save(a);
        return ActivityDtoMapper.toResponse(saved, mediaResolver.resolveImage(saved.getIconId()));
    }

    @Override
    public void delete(Long id) {
        Activity a = activityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Activity", id));

        mediaService.deleteImage(a.getIconId());
        activityRepository.deleteById(id);
    }
}
