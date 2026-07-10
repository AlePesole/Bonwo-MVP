package com.alessandropesole.bonwoapp.user.application.service;

import com.alessandropesole.bonwoapp.user.application.exception.AccountBannedException;
import com.alessandropesole.bonwoapp.user.application.exception.EmailAlreadyRegisteredException;
import com.alessandropesole.bonwoapp.user.application.exception.InvalidRefreshTokenException;
import com.alessandropesole.bonwoapp.user.application.exception.UsernameAlreadyTakenException;
import com.alessandropesole.bonwoapp.user.application.exception.UserNotFoundException;
import com.alessandropesole.bonwoapp.shared.infrastructure.security.JwtService;
import com.alessandropesole.bonwoapp.user.application.dto.*;
import com.alessandropesole.bonwoapp.user.application.mapper.UserDtoMapper;
import com.alessandropesole.bonwoapp.user.domain.model.AccountStatus;
import com.alessandropesole.bonwoapp.user.domain.model.User;
import com.alessandropesole.bonwoapp.user.domain.port.in.AuthUseCase;
import com.alessandropesole.bonwoapp.user.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService implements AuthUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Override
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyRegisteredException(request.email());
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new UsernameAlreadyTakenException(request.username());
        }
        User user = User.register(
                request.email(), passwordEncoder.encode(request.password()), request.username()
        );
        return UserDtoMapper.toResponse(userRepository.save(user));
    }

    @Override
    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UserNotFoundException(request.email()));

        if (user.getStatus() == AccountStatus.BANNED) throw new AccountBannedException();
        return buildAuthResponse(user);
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtService.isTokenValid(refreshToken)) throw new InvalidRefreshTokenException();
        String email = jwtService.extractSubject(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        Map<String, Object> claims = Map.of("role", user.getRole().name());
        String access = jwtService.generateToken(user.getEmail(), claims);
        String refresh = jwtService.generateRefreshToken(user.getEmail());
        return AuthResponse.of(access, refresh, UserDtoMapper.toResponse(user));
    }
}
