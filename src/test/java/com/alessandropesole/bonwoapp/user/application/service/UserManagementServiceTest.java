package com.alessandropesole.bonwoapp.user.application.service;

import com.alessandropesole.bonwoapp.shared.infrastructure.exception.ResourceNotFoundException;
import com.alessandropesole.bonwoapp.user.application.dto.AdminUpdateUserRequest;
import com.alessandropesole.bonwoapp.user.application.dto.UserResponse;
import com.alessandropesole.bonwoapp.user.domain.exception.AccountDeletedException;
import com.alessandropesole.bonwoapp.user.domain.exception.AlreadyBannedException;
import com.alessandropesole.bonwoapp.user.domain.exception.InvalidUsernameException;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserManagementService userManagementService;

    private User activeUser() {
        return User.reconstitute(1L, "user@example.com", "hashed", "johndoe",
                UserRole.USER, AccountStatus.ACTIVE, UserProfile.empty(), Instant.now());
    }

    @Test
    void listUsers_mapsPageOfUsersToResponses() {
        Pageable pageable = Pageable.ofSize(20);
        when(userRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(activeUser())));

        Page<UserResponse> result = userManagementService.listUsers(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).username()).isEqualTo("johndoe");
    }

    @Test
    void getUserById_returnsUserWhenFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser()));

        UserResponse response = userManagementService.getUserById(1L);

        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    void getUserById_throwsWhenNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userManagementService.getUserById(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void adminUpdateUser_updatesUsernameAndBioWhenBothProvided() {
        User user = activeUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = userManagementService.adminUpdateUser(1L,
                new AdminUpdateUserRequest("janedoe", "new bio"));

        assertThat(response.username()).isEqualTo("janedoe");
        assertThat(user.getProfile().getBio()).isEqualTo("new bio");
    }

    @Test
    void adminUpdateUser_leavesFieldsUnchangedWhenBothNull() {
        User user = activeUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = userManagementService.adminUpdateUser(1L,
                new AdminUpdateUserRequest(null, null));

        assertThat(response.username()).isEqualTo("johndoe");
        verify(userRepository).save(user);
    }

    @Test
    void adminUpdateUser_propagatesInvalidUsernameException() {
        User user = activeUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userManagementService.adminUpdateUser(1L,
                new AdminUpdateUserRequest("invalid username!", null)))
                .isInstanceOf(InvalidUsernameException.class);
    }

    @Test
    void adminUpdateUser_throwsWhenUserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userManagementService.adminUpdateUser(1L,
                new AdminUpdateUserRequest("janedoe", null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void banUser_bansAndSaves() {
        User user = activeUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userManagementService.banUser(1L);

        assertThat(user.getStatus()).isEqualTo(AccountStatus.BANNED);
        verify(userRepository).save(user);
    }

    @Test
    void banUser_propagatesAlreadyBannedException() {
        User user = User.reconstitute(1L, "user@example.com", "hashed", "johndoe",
                UserRole.USER, AccountStatus.BANNED, UserProfile.empty(), Instant.now());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userManagementService.banUser(1L))
                .isInstanceOf(AlreadyBannedException.class);
    }

    @Test
    void banUser_throwsWhenUserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userManagementService.banUser(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void unbanUser_restoresActiveStatus() {
        User user = User.reconstitute(1L, "user@example.com", "hashed", "johndoe",
                UserRole.USER, AccountStatus.BANNED, UserProfile.empty(), Instant.now());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userManagementService.unbanUser(1L);

        assertThat(user.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        verify(userRepository).save(user);
    }

    @Test
    void deleteUser_softDeletesAndSaves() {
        User user = activeUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userManagementService.deleteUser(1L);

        assertThat(user.getStatus()).isEqualTo(AccountStatus.DELETED);
        verify(userRepository).save(user);
    }

    @Test
    void changeRole_throwsWhenAccountDeleted() {
        User user = User.reconstitute(1L, "user@example.com", "hashed", "johndoe",
                UserRole.USER, AccountStatus.DELETED, UserProfile.empty(), Instant.now());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userManagementService.changeRole(1L, UserRole.ADMIN))
                .isInstanceOf(AccountDeletedException.class);

        verify(userRepository, never()).save(any());
    }
}
