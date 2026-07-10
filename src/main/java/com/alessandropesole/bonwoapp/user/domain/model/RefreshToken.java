package com.alessandropesole.bonwoapp.user.domain.model;

import com.alessandropesole.bonwoapp.shared.domain.AggregateRoot;
import lombok.Getter;

import java.time.Instant;

@Getter
public class RefreshToken extends AggregateRoot {

    private Long id;
    private Long userId;
    private String tokenId;
    private Instant expiresAt;
    private boolean revoked;
    private Instant createdAt;

    public static RefreshToken issue(Long userId, String tokenId, Instant expiresAt) {
        RefreshToken rt = new RefreshToken();
        rt.userId = userId;
        rt.tokenId = tokenId;
        rt.expiresAt = expiresAt;
        rt.revoked = false;
        rt.createdAt = Instant.now();
        return rt;
    }

    public static RefreshToken reconstitute(Long id, Long userId, String tokenId,
                                             Instant expiresAt, boolean revoked, Instant createdAt) {
        RefreshToken rt = new RefreshToken();
        rt.id = id;
        rt.userId = userId;
        rt.tokenId = tokenId;
        rt.expiresAt = expiresAt;
        rt.revoked = revoked;
        rt.createdAt = createdAt;
        return rt;
    }

    public void revoke() {
        this.revoked = true;
    }

    public boolean isUsable() {
        return !revoked && Instant.now().isBefore(expiresAt);
    }
}
