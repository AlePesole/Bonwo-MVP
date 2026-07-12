package com.alessandropesole.bonwoapp.user.application.service;

import com.alessandropesole.bonwoapp.shared.infrastructure.security.JwtService;
import com.alessandropesole.bonwoapp.user.application.dto.AuthRequest;
import com.alessandropesole.bonwoapp.user.application.dto.AuthResponse;
import com.alessandropesole.bonwoapp.user.application.dto.RegisterRequest;
import com.alessandropesole.bonwoapp.user.application.dto.UserResponse;
import com.alessandropesole.bonwoapp.user.application.exception.AccountBannedException;
import com.alessandropesole.bonwoapp.user.application.exception.EmailAlreadyRegisteredException;
import com.alessandropesole.bonwoapp.user.application.exception.InvalidRefreshTokenException;
import com.alessandropesole.bonwoapp.user.application.exception.UserNotFoundException;
import com.alessandropesole.bonwoapp.user.application.exception.UsernameAlreadyTakenException;
import com.alessandropesole.bonwoapp.user.domain.model.AccountStatus;
import com.alessandropesole.bonwoapp.user.domain.model.RefreshToken;
import com.alessandropesole.bonwoapp.user.domain.model.User;
import com.alessandropesole.bonwoapp.user.domain.model.UserProfile;
import com.alessandropesole.bonwoapp.user.domain.model.UserRole;
import com.alessandropesole.bonwoapp.user.domain.port.out.RefreshTokenRepository;
import com.alessandropesole.bonwoapp.user.domain.port.out.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private User activeUser() {
        return User.reconstitute(1L, "user@example.com", "hashed", "johndoe",
                UserRole.USER, AccountStatus.ACTIVE, UserProfile.empty(), Instant.now());
    }

    @Test
    void register_encodesPasswordAndSavesNewUser() {
        RegisterRequest req = new RegisterRequest("user@example.com", "password123", "johndoe");
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("johndoe")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = authService.register(req);

        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.username()).isEqualTo("johndoe");
        verify(passwordEncoder).encode("password123");
    }

    @Test
    void register_rejectsDuplicateEmailBeforeEncodingPassword() {
        RegisterRequest req = new RegisterRequest("user@example.com", "password123", "johndoe");
        when(userRepository.existsByEmail("user@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(EmailAlreadyRegisteredException.class);

        verifyNoInteractions(passwordEncoder);
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_rejectsDuplicateUsername() {
        RegisterRequest req = new RegisterRequest("user@example.com", "password123", "johndoe");
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("johndoe")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(UsernameAlreadyTakenException.class);

        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void login_authenticatesAndReturnsTokensForActiveUser() {
        AuthRequest req = new AuthRequest("user@example.com", "password123");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(activeUser()));
        when(jwtService.generateToken(eq("user@example.com"), any())).thenReturn("access-token");
        when(jwtService.generateRefreshToken("user@example.com")).thenReturn("refresh-token");
        when(jwtService.extractId("refresh-token")).thenReturn("token-id");
        when(jwtService.extractExpiration("refresh-token")).thenReturn(Instant.now().plusSeconds(3600));

        AuthResponse response = authService.login(req);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        verify(authenticationManager).authenticate(any());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void login_throwsWhenUserNotFoundAfterAuthentication() {
        AuthRequest req = new AuthRequest("user@example.com", "password123");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void login_throwsAccountBannedExceptionForBannedUser() {
        AuthRequest req = new AuthRequest("user@example.com", "password123");
        User banned = User.reconstitute(1L, "user@example.com", "hashed", "johndoe",
                UserRole.USER, AccountStatus.BANNED, UserProfile.empty(), Instant.now());
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(banned));

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(AccountBannedException.class);

        verifyNoInteractions(jwtService);
    }

    @Test
    void refreshToken_rotatesTokenAndIssuesNewPair() {
        RefreshToken stored = RefreshToken.issue(1L, "token-id", Instant.now().plusSeconds(3600));
        when(jwtService.isTokenValid("old-refresh")).thenReturn(true);
        when(jwtService.isRefreshToken("old-refresh")).thenReturn(true);
        when(jwtService.extractId("old-refresh")).thenReturn("token-id");
        when(refreshTokenRepository.findByTokenId("token-id")).thenReturn(Optional.of(stored));
        when(jwtService.extractSubject("old-refresh")).thenReturn("user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(activeUser()));
        when(jwtService.generateToken(any(), any())).thenReturn("new-access");
        when(jwtService.generateRefreshToken(any())).thenReturn("new-refresh");
        when(jwtService.extractId("new-refresh")).thenReturn("new-token-id");
        when(jwtService.extractExpiration("new-refresh")).thenReturn(Instant.now().plusSeconds(3600));

        AuthResponse response = authService.refreshToken("old-refresh");

        assertThat(response.accessToken()).isEqualTo("new-access");
        assertThat(stored.isRevoked()).isTrue();

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0)).isSameAs(stored);
    }

    @Test
    void refreshToken_rejectsTokenThatIsNotAValidJwt() {
        when(jwtService.isTokenValid("bad-token")).thenReturn(false);

        assertThatThrownBy(() -> authService.refreshToken("bad-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void refreshToken_rejectsTokenThatIsNotARefreshTypeToken() {
        when(jwtService.isTokenValid("access-token")).thenReturn(true);
        when(jwtService.isRefreshToken("access-token")).thenReturn(false);

        assertThatThrownBy(() -> authService.refreshToken("access-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void refreshToken_rejectsWhenNotFoundInStore() {
        when(jwtService.isTokenValid("old-refresh")).thenReturn(true);
        when(jwtService.isRefreshToken("old-refresh")).thenReturn(true);
        when(jwtService.extractId("old-refresh")).thenReturn("token-id");
        when(refreshTokenRepository.findByTokenId("token-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refreshToken("old-refresh"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void refreshToken_rejectsWhenStoredTokenIsAlreadyRevoked() {
        RefreshToken revoked = RefreshToken.issue(1L, "token-id", Instant.now().plusSeconds(3600));
        revoked.revoke();
        when(jwtService.isTokenValid("old-refresh")).thenReturn(true);
        when(jwtService.isRefreshToken("old-refresh")).thenReturn(true);
        when(jwtService.extractId("old-refresh")).thenReturn("token-id");
        when(refreshTokenRepository.findByTokenId("token-id")).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> authService.refreshToken("old-refresh"))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void refreshToken_rejectsWhenStoredTokenIsExpired() {
        RefreshToken expired = RefreshToken.reconstitute(1L, 1L, "token-id",
                Instant.now().minusSeconds(1), false, Instant.now());
        when(jwtService.isTokenValid("old-refresh")).thenReturn(true);
        when(jwtService.isRefreshToken("old-refresh")).thenReturn(true);
        when(jwtService.extractId("old-refresh")).thenReturn("token-id");
        when(refreshTokenRepository.findByTokenId("token-id")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> authService.refreshToken("old-refresh"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void refreshToken_stillRevokesPresentedTokenEvenIfUserNoLongerExists() {

        RefreshToken stored = RefreshToken.issue(1L, "token-id", Instant.now().plusSeconds(3600));
        when(jwtService.isTokenValid("old-refresh")).thenReturn(true);
        when(jwtService.isRefreshToken("old-refresh")).thenReturn(true);
        when(jwtService.extractId("old-refresh")).thenReturn("token-id");
        when(refreshTokenRepository.findByTokenId("token-id")).thenReturn(Optional.of(stored));
        when(jwtService.extractSubject("old-refresh")).thenReturn("gone@example.com");
        when(userRepository.findByEmail("gone@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refreshToken("old-refresh"))
                .isInstanceOf(UserNotFoundException.class);

        assertThat(stored.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(stored);
    }

    @Test
    void logout_revokesStoredTokenWhenFound() {
        RefreshToken stored = RefreshToken.issue(1L, "token-id", Instant.now().plusSeconds(3600));
        when(jwtService.isTokenValid("refresh-token")).thenReturn(true);
        when(jwtService.isRefreshToken("refresh-token")).thenReturn(true);
        when(jwtService.extractId("refresh-token")).thenReturn("token-id");
        when(refreshTokenRepository.findByTokenId("token-id")).thenReturn(Optional.of(stored));

        authService.logout("refresh-token");

        assertThat(stored.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(stored);
    }

    @Test
    void logout_silentlyNoOpsWhenTokenNotFoundInStore() {
        when(jwtService.isTokenValid("refresh-token")).thenReturn(true);
        when(jwtService.isRefreshToken("refresh-token")).thenReturn(true);
        when(jwtService.extractId("refresh-token")).thenReturn("token-id");
        when(refreshTokenRepository.findByTokenId("token-id")).thenReturn(Optional.empty());

        authService.logout("refresh-token");

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void logout_silentlyNoOpsWhenTokenIsInvalid() {
        when(jwtService.isTokenValid("garbage")).thenReturn(false);

        authService.logout("garbage");

        verifyNoInteractions(refreshTokenRepository);
    }
}
