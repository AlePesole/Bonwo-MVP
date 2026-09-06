package com.alessandropesole.bonwoapp.media.application.service;

import com.alessandropesole.bonwoapp.media.application.dto.ImageResponse;
import com.alessandropesole.bonwoapp.media.application.dto.VideoResponse;
import com.alessandropesole.bonwoapp.media.application.mapper.MediaDtoMapper;
import com.alessandropesole.bonwoapp.media.domain.model.Image;
import com.alessandropesole.bonwoapp.media.domain.model.Video;
import com.alessandropesole.bonwoapp.media.domain.port.out.ImageRepository;
import com.alessandropesole.bonwoapp.media.domain.port.out.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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

    /** Batched version of {@link #resolveImage} — one query for any number of ids. Missing/null
     *  ids are simply absent from the result map. */
    public Map<Long, ImageResponse> resolveImages(Set<Long> imageIds) {
        Set<Long> ids = imageIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) return Collections.emptyMap();
        return imageRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Image::getId, MediaDtoMapper::toResponse));
    }

    /** Batched version of {@link #resolveVideo} — one query for any number of ids. */
    public Map<Long, VideoResponse> resolveVideos(Set<Long> videoIds) {
        Set<Long> ids = videoIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) return Collections.emptyMap();
        return videoRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Video::getId, MediaDtoMapper::toResponse));
    }
}
