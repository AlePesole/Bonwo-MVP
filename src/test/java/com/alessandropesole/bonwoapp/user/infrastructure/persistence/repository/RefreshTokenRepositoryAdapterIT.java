package com.alessandropesole.bonwoapp.user.infrastructure.persistence.repository;

import com.alessandropesole.bonwoapp.support.AbstractIntegrationTest;
import com.alessandropesole.bonwoapp.user.domain.model.RefreshToken;
import com.alessandropesole.bonwoapp.user.domain.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({RefreshTokenRepositoryAdapter.class, UserRepositoryAdapter.class})
class RefreshTokenRepositoryAdapterIT extends AbstractIntegrationTest {

    @Autowired
    private RefreshTokenRepositoryAdapter refreshTokenRepository;
    @Autowired
    private UserRepositoryAdapter userRepository;

    @Test
    void save_persistsAndReloadsRefreshToken() {
        User user = userRepository.save(User.register("user@example.com", "hash", "johndoe"));

        RefreshToken saved = refreshTokenRepository.save(
                RefreshToken.issue(user.getId(), "token-id", Instant.now().plusSeconds(3600)));

        assertThat(saved.getId()).isNotNull();
        assertThat(refreshTokenRepository.findByTokenId("token-id")).isPresent();
    }

    @Test
    void findByTokenId_reflectsRevocationAfterSave() {
        User user = userRepository.save(User.register("user@example.com", "hash", "johndoe"));
        RefreshToken saved = refreshTokenRepository.save(
                RefreshToken.issue(user.getId(), "token-id", Instant.now().plusSeconds(3600)));

        saved.revoke();
        refreshTokenRepository.save(saved);

        RefreshToken reloaded = refreshTokenRepository.findByTokenId("token-id").orElseThrow();
        assertThat(reloaded.isRevoked()).isTrue();
    }

    @Test
    void findByTokenId_returnsEmptyWhenNotFound() {
        assertThat(refreshTokenRepository.findByTokenId("unknown")).isEmpty();
    }
}
