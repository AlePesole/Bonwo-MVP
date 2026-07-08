package com.alessandropesole.bonwoapp.media.application.mapper;

import com.alessandropesole.bonwoapp.media.application.dto.ImageResponse;
import com.alessandropesole.bonwoapp.media.application.dto.VideoResponse;
import com.alessandropesole.bonwoapp.media.domain.model.Image;
import com.alessandropesole.bonwoapp.media.domain.model.Video;

public final class MediaDtoMapper {

    private MediaDtoMapper() {
    }

    public static VideoResponse toResponse(Video v) {
        return new VideoResponse(v.getId(), v.getUrl(), v.getThumbnailUrl(),
                v.getDurationSeconds(), v.getStatus(), v.getCreatedAt());
    }

    public static ImageResponse toResponse(Image i) {
        return new ImageResponse(i.getId(), i.getUrl(), i.getCreatedAt());
    }
}
