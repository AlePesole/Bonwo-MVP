package com.alessandropesole.bonwoapp.media.application.service;

import com.alessandropesole.bonwoapp.media.application.dto.ImageResponse;
import com.alessandropesole.bonwoapp.media.application.dto.VideoResponse;
import com.alessandropesole.bonwoapp.media.domain.model.Image;
import com.alessandropesole.bonwoapp.media.domain.model.ImageStatus;
import com.alessandropesole.bonwoapp.media.domain.model.Video;
import com.alessandropesole.bonwoapp.media.domain.model.VideoStatus;
import com.alessandropesole.bonwoapp.media.domain.port.out.ImageRepository;
import com.alessandropesole.bonwoapp.media.domain.port.out.VideoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MediaResolverTest {

    @Mock
    private ImageRepository imageRepository;
    @Mock
    private VideoRepository videoRepository;

    @InjectMocks
    private MediaResolver mediaResolver;

    @Test
    void resolveImage_returnsNullWhenIdIsNull() {
        ImageResponse response = mediaResolver.resolveImage(null);

        assertThat(response).isNull();
        verifyNoInteractions(imageRepository);
    }

    @Test
    void resolveImage_returnsNullWhenNotFound() {
        when(imageRepository.findById(1L)).thenReturn(Optional.empty());

        assertThat(mediaResolver.resolveImage(1L)).isNull();
    }

    @Test
    void resolveImage_returnsResponseWhenFound() {
        Image image = Image.reconstitute(1L, 2L, "ext-id", "url", ImageStatus.ACTIVE,
                null, null, Instant.now());
        when(imageRepository.findById(1L)).thenReturn(Optional.of(image));

        ImageResponse response = mediaResolver.resolveImage(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.url()).isEqualTo("url");
    }

    @Test
    void resolveVideo_returnsNullWhenIdIsNull() {
        VideoResponse response = mediaResolver.resolveVideo(null);

        assertThat(response).isNull();
        verifyNoInteractions(videoRepository);
    }

    @Test
    void resolveVideo_returnsResponseWhenFound() {
        Video video = Video.reconstitute(1L, 2L, "ext-id", "url", "thumb", 30,
                VideoStatus.ACTIVE, null, null, Instant.now());
        when(videoRepository.findById(1L)).thenReturn(Optional.of(video));

        VideoResponse response = mediaResolver.resolveVideo(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.thumbnailUrl()).isEqualTo("thumb");
    }

    @Test
    void resolveImages_returnsEmptyMapForEmptySetWithoutHittingRepository() {
        Map<Long, ImageResponse> result = mediaResolver.resolveImages(Set.of());

        assertThat(result).isEmpty();
        verifyNoInteractions(imageRepository);
    }

    @Test
    void resolveImages_emptyResultMapDoesNotThrowOnNullKeyLookup() {
        // Reproduces the 500 seen when every exercise in a batch has no thumbnail: the caller does
        // thumbnailMap.get(exercise.getThumbnailId()), which is null — Map.of() rejects null keys
        // even on get(), so this must not be backed by Map.of().
        Map<Long, ImageResponse> result = mediaResolver.resolveImages(Set.of());

        assertThat(result.get(null)).isNull();
    }

    @Test
    void resolveImages_filtersNullIdsAndReturnsMapKeyedById() {
        Image image = Image.reconstitute(1L, 2L, "ext-id", "url", ImageStatus.ACTIVE,
                null, null, Instant.now());
        Set<Long> ids = new HashSet<>();
        ids.add(1L);
        ids.add(null);
        when(imageRepository.findAllById(Set.of(1L))).thenReturn(List.of(image));

        Map<Long, ImageResponse> result = mediaResolver.resolveImages(ids);

        assertThat(result).hasSize(1);
        assertThat(result.get(1L).url()).isEqualTo("url");
    }

    @Test
    void resolveVideos_returnsEmptyMapForEmptySetWithoutHittingRepository() {
        Map<Long, VideoResponse> result = mediaResolver.resolveVideos(Set.of());

        assertThat(result).isEmpty();
        verifyNoInteractions(videoRepository);
    }

    @Test
    void resolveVideos_emptyResultMapDoesNotThrowOnNullKeyLookup() {
        Map<Long, VideoResponse> result = mediaResolver.resolveVideos(Set.of());

        assertThat(result.get(null)).isNull();
    }

    @Test
    void resolveVideos_returnsMapKeyedById() {
        Video video = Video.reconstitute(1L, 2L, "ext-id", "url", "thumb", 30,
                VideoStatus.ACTIVE, null, null, Instant.now());
        when(videoRepository.findAllById(Set.of(1L))).thenReturn(List.of(video));

        Map<Long, VideoResponse> result = mediaResolver.resolveVideos(Set.of(1L));

        assertThat(result.get(1L).thumbnailUrl()).isEqualTo("thumb");
    }
}