package com.alessandropesole.bonwoapp.user.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenTest {

    @Test
    void issue_setsDefaultsAndUnrevokedState() {
        Instant expiresAt = Instant.now().plusSeconds(3600);

        RefreshToken token = RefreshToken.issue(1L, "token-id", expiresAt);

        assertThat(token.getUserId()).isEqualTo(1L);
        assertThat(token.getTokenId()).isEqualTo("token-id");
        assertThat(token.isRevoked()).isFalse();
        assertThat(token.getCreatedAt()).isNotNull();
    }

    @Test
    void revoke_setsRevokedTrue() {
        RefreshToken token = RefreshToken.issue(1L, "token-id", Instant.now().plusSeconds(3600));

        token.revoke();

        assertThat(token.isRevoked()).isTrue();
    }

    @Test
    void isUsable_trueWhenNotRevokedAndNotExpired() {
        RefreshToken token = RefreshToken.issue(1L, "token-id", Instant.now().plusSeconds(3600));

        assertThat(token.isUsable()).isTrue();
    }

    @Test
    void isUsable_falseWhenRevoked() {
        RefreshToken token = RefreshToken.issue(1L, "token-id", Instant.now().plusSeconds(3600));
        token.revoke();

        assertThat(token.isUsable()).isFalse();
    }

    @Test
    void isUsable_falseWhenExpired() {
        RefreshToken token = RefreshToken.reconstitute(1L, 1L, "token-id",
                Instant.now().minusSeconds(1), false, Instant.now());

        assertThat(token.isUsable()).isFalse();
    }
}
