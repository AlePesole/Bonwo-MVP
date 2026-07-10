package com.alessandropesole.bonwoapp.user.domain.port.in;

import com.alessandropesole.bonwoapp.user.application.dto.AdminUpdateUserRequest;
import com.alessandropesole.bonwoapp.user.application.dto.UserResponse;
import com.alessandropesole.bonwoapp.user.domain.model.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserManagementUseCase {
    Page<UserResponse> listUsers(Pageable pageable);

    UserResponse getUserById(Long userId);

    UserResponse adminUpdateUser(Long userId, AdminUpdateUserRequest request);

    void banUser(Long userId);

    void unbanUser(Long userId);

    void deleteUser(Long userId);

    void changeRole(Long userId, UserRole newRole);
}
