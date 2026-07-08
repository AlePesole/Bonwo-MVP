package com.alessandropesole.bonwoapp.media.application.service;

import com.alessandropesole.bonwoapp.media.application.dto.ImageResponse;
import com.alessandropesole.bonwoapp.media.application.dto.VideoResponse;
import com.alessandropesole.bonwoapp.media.application.mapper.MediaDtoMapper;
import com.alessandropesole.bonwoapp.media.domain.port.out.ImageRepository;
import com.alessandropesole.bonwoapp.media.domain.port.out.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Shared helper to resolve media ids to response DTOs.
 * Used by any service that needs to include image or video details in a response.
 */
@Component
@RequiredArgsConstructor
public class MediaResolver {

    private final ImageRepository imageRepository;
    private final VideoRepository videoRepository;

    public ImageResponse resolveImage(Long imageId) {
        if (imageId == null) return null;
        return imageRepository.findById(imageId)
                .map(MediaDtoMapper::toResponse)
                .orElse(null);
    }

    public VideoResponse resolveVideo(Long videoId) {
        if (videoId == null) return null;
        return videoRepository.findById(videoId)
                .map(MediaDtoMapper::toResponse)
                .orElse(null);
    }
}
