package com.alessandropesole.bonwoapp.media.infrastructure.persistence.entity;

import com.alessandropesole.bonwoapp.media.domain.model.VideoStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "videos", indexes = {
        @Index(name = "idx_videos_owner", columnList = "owner_id"),
        @Index(name = "idx_videos_upload_token", columnList = "upload_token"),
        @Index(name = "idx_videos_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;
    @Column(name = "external_id", nullable = false, length = 500)
    private String externalId;
    @Column(length = 1000)
    private String url;
    @Column(name = "thumbnail_url", length = 1000)
    private String thumbnailUrl;
    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private VideoStatus status;

    @Column(name = "upload_token", unique = true, length = 36)
    private String uploadToken;
    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
