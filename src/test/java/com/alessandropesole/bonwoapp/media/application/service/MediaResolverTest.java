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
import java.util.Optional;

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
}