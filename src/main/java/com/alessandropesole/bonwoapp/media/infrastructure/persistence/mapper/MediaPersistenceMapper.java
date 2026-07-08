package com.alessandropesole.bonwoapp.media.infrastructure.persistence.mapper;

import com.alessandropesole.bonwoapp.media.domain.model.Image;
import com.alessandropesole.bonwoapp.media.domain.model.Video;
import com.alessandropesole.bonwoapp.media.infrastructure.persistence.entity.ImageJpaEntity;
import com.alessandropesole.bonwoapp.media.infrastructure.persistence.entity.VideoJpaEntity;

public final class MediaPersistenceMapper {

    private MediaPersistenceMapper() {
    }

    public static Video toDomain(VideoJpaEntity e) {
        return Video.reconstitute(e.getId(), e.getOwnerId(), e.getExternalId(),
                e.getUrl(), e.getThumbnailUrl(), e.getDurationSeconds(),
                e.getStatus(), e.getUploadToken(), e.getExpiresAt(), e.getCreatedAt());
    }

    public static VideoJpaEntity toEntity(Video v) {
        return VideoJpaEntity.builder()
                .id(v.getId()).ownerId(v.getOwnerId()).externalId(v.getExternalId())
                .url(v.getUrl()).thumbnailUrl(v.getThumbnailUrl())
                .durationSeconds(v.getDurationSeconds()).status(v.getStatus())
                .uploadToken(v.getUploadToken()).expiresAt(v.getExpiresAt())
                .createdAt(v.getCreatedAt()).build();
    }

    public static Image toDomain(ImageJpaEntity e) {
        return Image.reconstitute(e.getId(), e.getOwnerId(), e.getExternalId(),
                e.getUrl(), e.getStatus(), e.getUploadToken(), e.getExpiresAt(), e.getCreatedAt());
    }

    public static ImageJpaEntity toEntity(Image i) {
        return ImageJpaEntity.builder()
                .id(i.getId()).ownerId(i.getOwnerId()).externalId(i.getExternalId())
                .url(i.getUrl()).status(i.getStatus())
                .uploadToken(i.getUploadToken()).expiresAt(i.getExpiresAt())
                .createdAt(i.getCreatedAt()).build();
    }
}
