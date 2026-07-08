package com.alessandropesole.bonwoapp.media.domain.model;

import com.alessandropesole.bonwoapp.media.domain.exception.VideoNotReadyException;
import com.alessandropesole.bonwoapp.shared.domain.AggregateRoot;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class Video extends AggregateRoot {

    private Long id;
    private Long ownerId;
    private String externalId;
    private String url;
    private String thumbnailUrl;
    private Integer durationSeconds;
    private VideoStatus status;
    private String uploadToken;
    private Instant expiresAt;
    private Instant createdAt;

    /**
     * Creates a PENDING video just uploaded to the provider.
     * Generates an uploadToken the entity creation request must include.
     */
    public static Video createPending(Long ownerId, String externalId,
                                       String url, String thumbnailUrl,
                                       Integer durationSeconds, Long ttlMinutes) {
        if (ownerId    == null) throw new IllegalArgumentException("ownerId required");
        if (externalId == null || externalId.isBlank())
            throw new IllegalArgumentException("externalId required");

        Video v = new Video();
        v.ownerId         = ownerId;
        v.externalId      = externalId;
        v.url             = url;
        v.thumbnailUrl    = thumbnailUrl;
        v.durationSeconds = durationSeconds;
        v.status          = VideoStatus.PENDING;
        v.uploadToken     = UUID.randomUUID().toString();
        v.expiresAt       = Instant.now().plusSeconds(ttlMinutes * 60);
        v.createdAt       = Instant.now();
        return v;
    }

    public static Video reconstitute(Long id, Long ownerId, String externalId,
                                      String url, String thumbnailUrl,
                                      Integer durationSeconds, VideoStatus status,
                                      String uploadToken, Instant expiresAt,
                                      Instant createdAt) {
        Video v = new Video();
        v.id              = id;
        v.ownerId         = ownerId;
        v.externalId      = externalId;
        v.url             = url;
        v.thumbnailUrl    = thumbnailUrl;
        v.durationSeconds = durationSeconds;
        v.status          = status;
        v.uploadToken     = uploadToken;
        v.expiresAt       = expiresAt;
        v.createdAt       = createdAt;
        return v;
    }

    /**
     * Claims this video for an entity.
     * Clears the uploadToken and expiresAt — video is now ACTIVE.
     */
    public void activate() {
        this.status      = VideoStatus.ACTIVE;
        this.uploadToken = null;
        this.expiresAt   = null;
    }

    public void markFailed() {
        this.status = VideoStatus.FAILED;
    }

    public boolean isPending()  { return status == VideoStatus.PENDING; }
    public boolean isActive()   { return status == VideoStatus.ACTIVE; }
    public boolean isExpired()  { return expiresAt != null && Instant.now().isAfter(expiresAt); }

    public void assertActive() {
        if (!isActive()) throw new VideoNotReadyException(id);
    }

    public boolean isOwnedBy(Long userId) {
        return ownerId != null && ownerId.equals(userId);
    }
}
