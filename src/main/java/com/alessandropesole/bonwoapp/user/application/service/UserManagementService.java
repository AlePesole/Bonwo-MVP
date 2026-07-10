package com.alessandropesole.bonwoapp.user.application.service;

import com.alessandropesole.bonwoapp.shared.infrastructure.exception.ResourceNotFoundException;
import com.alessandropesole.bonwoapp.user.application.dto.AdminUpdateUserRequest;
import com.alessandropesole.bonwoapp.user.application.dto.UserResponse;
import com.alessandropesole.bonwoapp.user.application.mapper.UserDtoMapper;
import com.alessandropesole.bonwoapp.user.domain.model.User;
import com.alessandropesole.bonwoapp.user.domain.model.UserRole;
import com.alessandropesole.bonwoapp.user.domain.port.in.UserManagementUseCase;
import com.alessandropesole.bonwoapp.user.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserManagementService implements UserManagementUseCase {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserDtoMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long userId) {
        return UserDtoMapper.toResponse(findOrThrow(userId));
    }

    @Override
    public UserResponse adminUpdateUser(Long userId, AdminUpdateUserRequest request) {
        User user = findOrThrow(userId);
        if (request.username() != null) user.updateUsername(request.username());
        if (request.bio() != null) {
            user.updateProfile(user.getProfile().applyUpdate(request.bio(), null, null, null, null));
        }
        return UserDtoMapper.toResponse(userRepository.save(user));
    }

    @Override
    public void banUser(Long userId) {
        User u = findOrThrow(userId);
        u.ban();
        userRepository.save(u);
    }

    @Override
    public void unbanUser(Long userId) {
        User u = findOrThrow(userId);
        u.unban();
        userRepository.save(u);
    }

    @Override
    public void deleteUser(Long userId) {
        User u = findOrThrow(userId);
        u.softDelete();
        userRepository.save(u);
    }

    @Override
    public void changeRole(Long userId, UserRole newRole) {
        User u = findOrThrow(userId);
        u.changeRole(newRole);
        userRepository.save(u);
    }

    private User findOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
}
