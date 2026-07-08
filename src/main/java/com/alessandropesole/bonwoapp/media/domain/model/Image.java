package com.alessandropesole.bonwoapp.media.domain.model;

import com.alessandropesole.bonwoapp.shared.domain.AggregateRoot;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class Image extends AggregateRoot {

    private Long id;
    private Long ownerId;
    private String externalId;
    private String url;
    private ImageStatus status;
    private String uploadToken;
    private Instant expiresAt;
    private Instant createdAt;

    private static final long PENDING_TTL_MINUTES = 15;

    public static Image createPending(Long ownerId, String externalId, String url) {
        if (ownerId    == null) throw new IllegalArgumentException("ownerId required");
        if (externalId == null || externalId.isBlank())
            throw new IllegalArgumentException("externalId required");

        Image i = new Image();
        i.ownerId     = ownerId;
        i.externalId  = externalId;
        i.url         = url;
        i.status      = ImageStatus.PENDING;
        i.uploadToken = UUID.randomUUID().toString();
        i.expiresAt   = Instant.now().plusSeconds(PENDING_TTL_MINUTES * 60);
        i.createdAt   = Instant.now();
        return i;
    }

    public static Image reconstitute(Long id, Long ownerId, String externalId,
                                      String url, ImageStatus status,
                                      String uploadToken, Instant expiresAt,
                                      Instant createdAt) {
        Image i = new Image();
        i.id          = id;
        i.ownerId     = ownerId;
        i.externalId  = externalId;
        i.url         = url;
        i.status      = status;
        i.uploadToken = uploadToken;
        i.expiresAt   = expiresAt;
        i.createdAt   = createdAt;
        return i;
    }

    public void activate() {
        this.status      = ImageStatus.ACTIVE;
        this.uploadToken = null;
        this.expiresAt   = null;
    }

    public boolean isPending()  { return status == ImageStatus.PENDING; }
    public boolean isActive()   { return status == ImageStatus.ACTIVE; }
    public boolean isExpired()  { return expiresAt != null && Instant.now().isAfter(expiresAt); }

    public boolean isOwnedBy(Long userId) {
        return ownerId != null && ownerId.equals(userId);
    }
}
