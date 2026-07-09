package com.alessandropesole.bonwoapp.user.domain.port.in;

import com.alessandropesole.bonwoapp.user.application.dto.AuthRequest;
import com.alessandropesole.bonwoapp.user.application.dto.AuthResponse;
import com.alessandropesole.bonwoapp.user.application.dto.RegisterRequest;
import com.alessandropesole.bonwoapp.user.application.dto.UserResponse;

public interface AuthUseCase {
    UserResponse register(RegisterRequest request);

    AuthResponse login(AuthRequest request);

    AuthResponse refreshToken(String refreshToken);
}
