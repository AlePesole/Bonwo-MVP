package com.alessandropesole.bonwoapp.user.application.service;

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
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentUserResolverTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserDetails principal;

    @InjectMocks
    private CurrentUserResolver currentUserResolver;

    @Test
    void resolveId_returnsUserIdWhenFoundByEmail() {
        User user = User.reconstitute(7L, "user@example.com", "hashed", "johndoe",
                UserRole.USER, AccountStatus.ACTIVE, UserProfile.empty(), Instant.now());
        when(principal.getUsername()).thenReturn("user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        Long id = currentUserResolver.resolveId(principal);

        assertThat(id).isEqualTo(7L);
    }

    @Test
    void resolveId_throwsWhenUserNotFound() {
        when(principal.getUsername()).thenReturn("ghost@example.com");
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> currentUserResolver.resolveId(principal))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ghost@example.com");
    }
}
