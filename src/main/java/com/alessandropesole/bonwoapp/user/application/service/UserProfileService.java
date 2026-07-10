package com.alessandropesole.bonwoapp.user.application.service;

import com.alessandropesole.bonwoapp.catalog.application.service.CatalogValidator;
import com.alessandropesole.bonwoapp.catalog.domain.port.out.ActivityRepository;
import com.alessandropesole.bonwoapp.media.application.dto.ImageResponse;
import com.alessandropesole.bonwoapp.media.application.service.MediaResolver;
import com.alessandropesole.bonwoapp.media.application.service.MediaService;
import com.alessandropesole.bonwoapp.shared.infrastructure.exception.ResourceNotFoundException;
import com.alessandropesole.bonwoapp.user.application.dto.UpdateProfileRequest;
import com.alessandropesole.bonwoapp.user.application.dto.UserProfileResponse;
import com.alessandropesole.bonwoapp.user.application.mapper.UserProfileDtoMapper;
import com.alessandropesole.bonwoapp.user.domain.model.User;
import com.alessandropesole.bonwoapp.user.domain.model.UserProfile;
import com.alessandropesole.bonwoapp.user.domain.port.in.UserProfileUseCase;
import com.alessandropesole.bonwoapp.user.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.alessandropesole.bonwoapp.catalog.application.dto.ActivityResponse;
import com.alessandropesole.bonwoapp.catalog.application.mapper.ActivityDtoMapper;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


@Service
@RequiredArgsConstructor
@Transactional
public class UserProfileService implements UserProfileUseCase {

    private final UserRepository userRepository;
    private final MediaResolver mediaResolver;
    private final MediaService mediaService;
    private final CatalogValidator catalogValidator;
    private final ActivityRepository activityRepository;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(Long userId) {
        User user = findOrThrow(userId);
        return toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getPublicProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));
        return toResponse(user);
    }

    @Override
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest req) {
        User user = findOrThrow(userId);

        if (req.activityIds() != null) {
            catalogValidator.validateActivities(req.activityIds());
        }

        Long newAvatarId = null;
        if (req.avatarUploadToken() != null && !req.avatarUploadToken().isBlank()) {
            newAvatarId = mediaService.claimImage(req.avatarUploadToken(), userId);
            mediaService.deleteImageIfOwner(user.getProfile().getAvatarId(), userId);
        }

        Set<Long> activityIds = req.activityIds() != null
                ? new LinkedHashSet<>(req.activityIds()) : null;

        UserProfile updatedProfile = user.getProfile().applyUpdate(
                req.bio(), req.ageYears(), req.heightCm(), req.weightKg(), activityIds);

        if (newAvatarId != null) {
            updatedProfile = updatedProfile.withAvatarId(newAvatarId);
        }

        user.updateProfile(updatedProfile);

        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    private UserProfileResponse toResponse(User user) {
        ImageResponse avatar = mediaResolver.resolveImage(user.getProfile().getAvatarId());
        List<ActivityResponse> activities = resolveActivities(user);
        return UserProfileDtoMapper.toResponse(user, avatar, activities);
    }

    private List<ActivityResponse> resolveActivities(User user) {
        Set<Long> ids = user.getProfile().getActivityIds();
        if (ids.isEmpty()) return List.of();
        return activityRepository.findAllById(ids).stream()
                .map(a -> ActivityDtoMapper.toResponse(a, mediaResolver.resolveImage(a.getIconId())))
                .toList();
    }

    private User findOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
}
