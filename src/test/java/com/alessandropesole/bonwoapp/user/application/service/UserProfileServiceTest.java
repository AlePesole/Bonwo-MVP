package com.alessandropesole.bonwoapp.user.application.service;

import com.alessandropesole.bonwoapp.catalog.application.service.CatalogValidator;
import com.alessandropesole.bonwoapp.catalog.domain.model.Activity;
import com.alessandropesole.bonwoapp.catalog.domain.port.out.ActivityRepository;
import com.alessandropesole.bonwoapp.media.application.dto.ImageResponse;
import com.alessandropesole.bonwoapp.media.application.service.MediaResolver;
import com.alessandropesole.bonwoapp.media.application.service.MediaService;
import com.alessandropesole.bonwoapp.shared.infrastructure.exception.ResourceNotFoundException;
import com.alessandropesole.bonwoapp.user.application.dto.UpdateProfileRequest;
import com.alessandropesole.bonwoapp.user.application.dto.UserProfileResponse;
import com.alessandropesole.bonwoapp.user.domain.model.AccountStatus;
import com.alessandropesole.bonwoapp.user.domain.model.User;
import com.alessandropesole.bonwoapp.user.domain.model.UserProfile;
import com.alessandropesole.bonwoapp.user.domain.model.UserRole;
import com.alessandropesole.bonwoapp.user.domain.port.out.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private MediaResolver mediaResolver;
    @Mock
    private MediaService mediaService;
    @Mock
    private CatalogValidator catalogValidator;
    @Mock
    private ActivityRepository activityRepository;

    @InjectMocks
    private UserProfileService userProfileService;

    private User userWithProfile(UserProfile profile) {
        return User.reconstitute(1L, "user@example.com", "hashed", "johndoe",
                UserRole.USER, AccountStatus.ACTIVE, profile, Instant.now());
    }

    @Test
    void getMyProfile_returnsProfileResponse() {
        User user = userWithProfile(UserProfile.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserProfileResponse response = userProfileService.getMyProfile(1L);

        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.username()).isEqualTo("johndoe");
    }

    @Test
    void getMyProfile_throwsWhenUserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userProfileService.getMyProfile(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getPublicProfile_returnsProfileByUsername() {
        User user = userWithProfile(UserProfile.empty());
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(user));

        UserProfileResponse response = userProfileService.getPublicProfile("johndoe");

        assertThat(response.username()).isEqualTo("johndoe");
    }

    @Test
    void getPublicProfile_throwsWhenUsernameNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userProfileService.getPublicProfile("unknown"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateProfile_appliesPartialUpdateWithoutTouchingAvatarOrActivities() {
        User user = userWithProfile(UserProfile.of(1L, "old bio", 30, 180, 80.0, null));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProfileRequest req = new UpdateProfileRequest(null, "new bio", null, null, null, null);
        UserProfileResponse response = userProfileService.updateProfile(1L, req);

        assertThat(response.bio()).isEqualTo("new bio");
        assertThat(response.ageYears()).isEqualTo(30);
        verifyNoInteractions(mediaService, catalogValidator);
    }

    @Test
    void updateProfile_validatesActivityIdsWhenProvided() {
        User user = userWithProfile(UserProfile.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        Activity activity = Activity.reconstitute(5L, "Running", "Cardio", 10L);
        when(activityRepository.findAllById(Set.of(5L))).thenReturn(List.of(activity));
        when(mediaResolver.resolveImage(10L)).thenReturn(new ImageResponse(10L, "url", null));
        when(mediaResolver.resolveImage(null)).thenReturn(null);

        UpdateProfileRequest req = new UpdateProfileRequest(null, null, null, null, null, Set.of(5L));
        UserProfileResponse response = userProfileService.updateProfile(1L, req);

        verify(catalogValidator).validateActivities(Set.of(5L));
        assertThat(response.activities()).hasSize(1);
        assertThat(response.activities().get(0).name()).isEqualTo("Running");
    }

    @Test
    void updateProfile_propagatesInvalidActivityIds() {
        User user = userWithProfile(UserProfile.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        doThrow(new ResourceNotFoundException("One or more activityIds do not exist in the catalog"))
                .when(catalogValidator).validateActivities(Set.of(999L));

        UpdateProfileRequest req = new UpdateProfileRequest(null, null, null, null, null, Set.of(999L));

        assertThatThrownBy(() -> userProfileService.updateProfile(1L, req))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateProfile_claimsNewAvatarAndDeletesOldOne() {
        User user = userWithProfile(UserProfile.of(1L, null, null, null, null, null));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mediaService.claimImage("new-token", 1L)).thenReturn(99L);
        when(mediaResolver.resolveImage(99L)).thenReturn(new ImageResponse(99L, "new-url", null));

        UpdateProfileRequest req = new UpdateProfileRequest("new-token", null, null, null, null, null);
        UserProfileResponse response = userProfileService.updateProfile(1L, req);

        assertThat(response.avatar().id()).isEqualTo(99L);
        verify(mediaService).claimImage("new-token", 1L);
        verify(mediaService).deleteImageIfOwner(1L, 1L);
    }

    @Test
    void updateProfile_doesNotTouchAvatarWhenTokenIsBlank() {
        User user = userWithProfile(UserProfile.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProfileRequest req = new UpdateProfileRequest("  ", null, null, null, null, null);
        userProfileService.updateProfile(1L, req);

        verifyNoInteractions(mediaService);
    }

    @Test
    void updateProfile_throwsWhenUserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        UpdateProfileRequest req = new UpdateProfileRequest(null, "bio", null, null, null, null);

        assertThatThrownBy(() -> userProfileService.updateProfile(1L, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
