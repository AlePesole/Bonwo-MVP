package com.alessandropesole.bonwoapp.media.application.service;

import com.alessandropesole.bonwoapp.media.application.dto.ImageUploadResponse;
import com.alessandropesole.bonwoapp.media.application.dto.VideoUploadResponse;
import com.alessandropesole.bonwoapp.media.domain.exception.MediaNotOwnedException;
import com.alessandropesole.bonwoapp.media.domain.model.Image;
import com.alessandropesole.bonwoapp.media.domain.model.ImageStatus;
import com.alessandropesole.bonwoapp.media.domain.model.Video;
import com.alessandropesole.bonwoapp.media.domain.model.VideoStatus;
import com.alessandropesole.bonwoapp.media.domain.port.out.ImageRepository;
import com.alessandropesole.bonwoapp.media.domain.port.out.MediaStoragePort;
import com.alessandropesole.bonwoapp.media.domain.port.out.VideoRepository;
import com.alessandropesole.bonwoapp.media.infrastructure.config.MediaProperties;
import com.alessandropesole.bonwoapp.shared.infrastructure.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

    @Mock
    private VideoRepository videoRepository;
    @Mock
    private ImageRepository imageRepository;
    @Mock
    private MediaStoragePort mediaStoragePort;
    @Mock
    private MediaProperties mediaProperties;

    @InjectMocks
    private MediaService mediaService;

    private static final Long OWNER_ID = 1L;

    @Test
    void uploadVideo_storesAndSavesPendingVideo() {
        MultipartFile file = new MockMultipartFile("file", "video.mp4", "video/mp4", "data".getBytes());
        when(mediaProperties.pendingTtlMinutes()).thenReturn(15L);
        when(mediaStoragePort.uploadVideo(any(), eq("video.mp4"), eq("video/mp4"), eq(OWNER_ID)))
                .thenReturn(new MediaStoragePort.UploadedVideo("ext-id", "url", "thumb", 30));
        when(videoRepository.save(any(Video.class))).thenAnswer(inv -> inv.getArgument(0));

        VideoUploadResponse response = mediaService.uploadVideo(file, OWNER_ID);

        assertThat(response.uploadToken()).isNotBlank();
        assertThat(response.thumbnailUrl()).isEqualTo("thumb");
        assertThat(response.durationSeconds()).isEqualTo(30);
    }

    @Test
    void uploadVideo_wrapsIOExceptionFromFile() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getBytes()).thenThrow(new IOException("boom"));

        assertThatThrownBy(() -> mediaService.uploadVideo(file, OWNER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(IOException.class);
    }

    @Test
    void uploadImage_storesAndSavesPendingImage() {
        MultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", "data".getBytes());
        when(mediaProperties.pendingTtlMinutes()).thenReturn(15L);
        when(mediaStoragePort.uploadImage(any(), eq("avatar.png"), eq("image/png"), eq(OWNER_ID)))
                .thenReturn(new MediaStoragePort.UploadedImage("ext-id", "url"));
        when(imageRepository.save(any(Image.class))).thenAnswer(inv -> inv.getArgument(0));

        ImageUploadResponse response = mediaService.uploadImage(file, OWNER_ID);

        assertThat(response.uploadToken()).isNotBlank();
        assertThat(response.url()).isEqualTo("url");
    }

    @Test
    void claimVideo_activatesAndReturnsIdWhenOwnedAndNotExpired() {
        Video pending = Video.reconstitute(5L, OWNER_ID, "ext-id", "url", "thumb", 10,
                VideoStatus.PENDING, "token", Instant.now().plusSeconds(60), Instant.now());
        when(videoRepository.findByUploadToken("token")).thenReturn(Optional.of(pending));
        when(videoRepository.save(any(Video.class))).thenAnswer(inv -> inv.getArgument(0));

        Long id = mediaService.claimVideo("token", OWNER_ID);

        assertThat(id).isEqualTo(5L);
        assertThat(pending.getStatus()).isEqualTo(VideoStatus.ACTIVE);
    }

    @Test
    void claimVideo_throwsWhenTokenNotFound() {
        when(videoRepository.findByUploadToken("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mediaService.claimVideo("missing", OWNER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void claimVideo_throwsWhenNotOwner() {
        Video pending = Video.reconstitute(5L, 999L, "ext-id", "url", "thumb", 10,
                VideoStatus.PENDING, "token", Instant.now().plusSeconds(60), Instant.now());
        when(videoRepository.findByUploadToken("token")).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> mediaService.claimVideo("token", OWNER_ID))
                .isInstanceOf(MediaNotOwnedException.class);
    }

    @Test
    void claimVideo_deletesAndThrowsWhenExpired() {
        Video expired = Video.reconstitute(5L, OWNER_ID, "ext-id", "url", "thumb", 10,
                VideoStatus.PENDING, "token", Instant.now().minusSeconds(60), Instant.now());
        when(videoRepository.findByUploadToken("token")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> mediaService.claimVideo("token", OWNER_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(mediaStoragePort).deleteVideo("ext-id");
        verify(videoRepository).deleteById(5L);
        verify(videoRepository, never()).save(any());
    }

    @Test
    void claimImage_activatesAndReturnsIdWhenOwnedAndNotExpired() {
        Image pending = Image.reconstitute(5L, OWNER_ID, "ext-id", "url", ImageStatus.PENDING,
                "token", Instant.now().plusSeconds(60), Instant.now());
        when(imageRepository.findByUploadToken("token")).thenReturn(Optional.of(pending));
        when(imageRepository.save(any(Image.class))).thenAnswer(inv -> inv.getArgument(0));

        Long id = mediaService.claimImage("token", OWNER_ID);

        assertThat(id).isEqualTo(5L);
        assertThat(pending.getStatus()).isEqualTo(ImageStatus.ACTIVE);
    }

    @Test
    void claimImage_throwsWhenTokenNotFound() {
        when(imageRepository.findByUploadToken("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mediaService.claimImage("missing", OWNER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void claimImage_throwsWhenNotOwner() {
        Image pending = Image.reconstitute(5L, 999L, "ext-id", "url", ImageStatus.PENDING,
                "token", Instant.now().plusSeconds(60), Instant.now());
        when(imageRepository.findByUploadToken("token")).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> mediaService.claimImage("token", OWNER_ID))
                .isInstanceOf(MediaNotOwnedException.class);
    }

    @Test
    void claimImage_deletesAndThrowsWhenExpired() {
        Image expired = Image.reconstitute(5L, OWNER_ID, "ext-id", "url", ImageStatus.PENDING,
                "token", Instant.now().minusSeconds(60), Instant.now());
        when(imageRepository.findByUploadToken("token")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> mediaService.claimImage("token", OWNER_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(mediaStoragePort).deleteImage("ext-id");
        verify(imageRepository).deleteById(5L);
        verify(imageRepository, never()).save(any());
    }

    @Test
    void deleteVideoIfOwner_doesNothingWhenIdIsNull() {
        mediaService.deleteVideoIfOwner(null, OWNER_ID);

        verifyNoInteractions(videoRepository, mediaStoragePort);
    }

    @Test
    void deleteVideoIfOwner_deletesWhenOwned() {
        Video video = Video.reconstitute(5L, OWNER_ID, "ext-id", "url", "thumb", 10,
                VideoStatus.ACTIVE, null, null, Instant.now());
        when(videoRepository.findById(5L)).thenReturn(Optional.of(video));

        mediaService.deleteVideoIfOwner(5L, OWNER_ID);

        verify(mediaStoragePort).deleteVideo("ext-id");
        verify(videoRepository).deleteById(5L);
    }

    @Test
    void deleteVideoIfOwner_skipsWhenNotOwned() {
        Video video = Video.reconstitute(5L, 999L, "ext-id", "url", "thumb", 10,
                VideoStatus.ACTIVE, null, null, Instant.now());
        when(videoRepository.findById(5L)).thenReturn(Optional.of(video));

        mediaService.deleteVideoIfOwner(5L, OWNER_ID);

        verify(mediaStoragePort, never()).deleteVideo(any());
        verify(videoRepository, never()).deleteById(any());
    }

    @Test
    void deleteImageIfOwner_doesNothingWhenIdIsNull() {
        mediaService.deleteImageIfOwner(null, OWNER_ID);

        verifyNoInteractions(imageRepository, mediaStoragePort);
    }

    @Test
    void deleteImageIfOwner_deletesWhenOwned() {
        Image image = Image.reconstitute(5L, OWNER_ID, "ext-id", "url", ImageStatus.ACTIVE,
                null, null, Instant.now());
        when(imageRepository.findById(5L)).thenReturn(Optional.of(image));

        mediaService.deleteImageIfOwner(5L, OWNER_ID);

        verify(mediaStoragePort).deleteImage("ext-id");
        verify(imageRepository).deleteById(5L);
    }

    @Test
    void deleteImageIfOwner_skipsWhenNotOwned() {
        Image image = Image.reconstitute(5L, 999L, "ext-id", "url", ImageStatus.ACTIVE,
                null, null, Instant.now());
        when(imageRepository.findById(5L)).thenReturn(Optional.of(image));

        mediaService.deleteImageIfOwner(5L, OWNER_ID);

        verify(mediaStoragePort, never()).deleteImage(any());
        verify(imageRepository, never()).deleteById(any());
    }

    @Test
    void deleteExpiredPendingVideos_deletesEachAndReturnsCount() {
        Video v1 = Video.reconstitute(1L, OWNER_ID, "ext-1", "url", "thumb", 10,
                VideoStatus.PENDING, "token", Instant.now().minusSeconds(60), Instant.now());
        Video v2 = Video.reconstitute(2L, OWNER_ID, "ext-2", "url", "thumb", 10,
                VideoStatus.PENDING, "token", Instant.now().minusSeconds(60), Instant.now());
        when(videoRepository.findAllExpiredPending()).thenReturn(List.of(v1, v2));

        int count = mediaService.deleteExpiredPendingVideos();

        assertThat(count).isEqualTo(2);
        verify(videoRepository).deleteById(1L);
        verify(videoRepository).deleteById(2L);
    }

    @Test
    void deleteExpiredPendingImages_deletesEachAndReturnsCount() {
        Image i1 = Image.reconstitute(1L, OWNER_ID, "ext-1", "url", ImageStatus.PENDING,
                "token", Instant.now().minusSeconds(60), Instant.now());
        when(imageRepository.findAllExpiredPending()).thenReturn(List.of(i1));

        int count = mediaService.deleteExpiredPendingImages();

        assertThat(count).isEqualTo(1);
        verify(imageRepository).deleteById(1L);
    }

    @Test
    void deleteImage_doesNothingWhenIdIsNull() {
        mediaService.deleteImage(null);

        verifyNoInteractions(imageRepository, mediaStoragePort);
    }

    @Test
    void deleteImage_deletesUnconditionallyWhenFound() {
        Image image = Image.reconstitute(5L, 999L, "ext-id", "url", ImageStatus.ACTIVE,
                null, null, Instant.now());
        when(imageRepository.findById(5L)).thenReturn(Optional.of(image));

        mediaService.deleteImage(5L);

        verify(mediaStoragePort).deleteImage("ext-id");
        verify(imageRepository).deleteById(5L);
    }
}