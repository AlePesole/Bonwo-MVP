package com.alessandropesole.bonwoapp.media.application.service;

import com.alessandropesole.bonwoapp.media.application.dto.ImageUploadResponse;
import com.alessandropesole.bonwoapp.media.application.dto.VideoUploadResponse;
import com.alessandropesole.bonwoapp.media.domain.exception.MediaNotOwnedException;
import com.alessandropesole.bonwoapp.media.infrastructure.config.MediaProperties;
import com.alessandropesole.bonwoapp.media.domain.model.Image;
import com.alessandropesole.bonwoapp.media.domain.model.Video;
import com.alessandropesole.bonwoapp.media.domain.port.out.ImageRepository;
import com.alessandropesole.bonwoapp.media.domain.port.out.MediaStoragePort;
import com.alessandropesole.bonwoapp.media.domain.port.out.VideoRepository;
import com.alessandropesole.bonwoapp.shared.infrastructure.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MediaService {

    private final VideoRepository videoRepository;
    private final ImageRepository imageRepository;
    private final MediaStoragePort mediaStoragePort;
    private final MediaProperties mediaProperties;

    public VideoUploadResponse uploadVideo(MultipartFile file, Long ownerId) {
        try {
            MediaStoragePort.UploadedVideo uploaded = mediaStoragePort.uploadVideo(
                    file.getBytes(), file.getOriginalFilename(),
                    file.getContentType(), ownerId);

            Video video = Video.createPending(ownerId, uploaded.externalId(),
                    uploaded.url(), uploaded.thumbnailUrl(), uploaded.durationSeconds(),
                    mediaProperties.pendingTtlMinutes());

            Video saved = videoRepository.save(video);
            log.info("Video uploaded — ownerId={} uploadToken={}", ownerId, saved.getUploadToken());

            return new VideoUploadResponse(
                    saved.getUploadToken(),
                    saved.getThumbnailUrl(),
                    saved.getDurationSeconds(),
                    saved.getExpiresAt()
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to read uploaded video", e);
        }
    }

    public ImageUploadResponse uploadImage(MultipartFile file, Long ownerId) {
        try {
            MediaStoragePort.UploadedImage uploaded = mediaStoragePort.uploadImage(
                    file.getBytes(), file.getOriginalFilename(),
                    file.getContentType(), ownerId);

            Image image = Image.createPending(ownerId, uploaded.externalId(), uploaded.url(),
                    mediaProperties.pendingTtlMinutes());
            Image saved = imageRepository.save(image);
            log.info("Image uploaded — ownerId={} uploadToken={}", ownerId, saved.getUploadToken());

            return new ImageUploadResponse(saved.getUploadToken(), saved.getUrl(), saved.getExpiresAt());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read uploaded image", e);
        }
    }

    public Long claimVideo(String uploadToken, Long ownerId) {
        Video video = videoRepository.findByUploadToken(uploadToken)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Video upload token not found or already used: " + uploadToken));

        if (!video.isOwnedBy(ownerId)) throw new MediaNotOwnedException();
        if (video.isExpired()) {
            deleteVideoInternal(video);
            throw new ResourceNotFoundException("Video upload token has expired");
        }

        video.activate();
        return videoRepository.save(video).getId();
    }

    public Long claimImage(String uploadToken, Long ownerId) {
        Image image = imageRepository.findByUploadToken(uploadToken)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Image upload token not found or already used: " + uploadToken));

        if (!image.isOwnedBy(ownerId)) throw new MediaNotOwnedException();
        if (image.isExpired()) {
            deleteImageInternal(image);
            throw new ResourceNotFoundException("Image upload token has expired");
        }

        image.activate();
        return imageRepository.save(image).getId();
    }

    public void verifyImageOwnership(Long imageId, Long ownerId) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image", imageId));
        if (!image.isOwnedBy(ownerId)) throw new MediaNotOwnedException();
    }

    public void deleteVideoIfOwner(Long videoId, Long ownerId) {
        if (videoId == null) return;
        videoRepository.findById(videoId).ifPresent(v -> {
            if (!v.isOwnedBy(ownerId)) {
                log.warn("Skipping video delete — videoId={} does not belong to ownerId={}", videoId, ownerId);
                return;
            }
            deleteVideoInternal(v);
        });
    }

    public void deleteImageIfOwner(Long imageId, Long ownerId) {
        if (imageId == null) return;
        imageRepository.findById(imageId).ifPresent(i -> {
            if (!i.isOwnedBy(ownerId)) {
                log.warn("Skipping image delete — imageId={} does not belong to ownerId={}", imageId, ownerId);
                return;
            }
            deleteImageInternal(i);
        });
    }

    public int deleteExpiredPendingVideos() {
        var expired = videoRepository.findAllExpiredPending();
        expired.forEach(this::deleteVideoInternal);
        return expired.size();
    }

    public int deleteExpiredPendingImages() {
        var expired = imageRepository.findAllExpiredPending();
        expired.forEach(this::deleteImageInternal);
        return expired.size();
    }

    public int deleteOrphanedImages() {
        var threshold = Instant.now().minus(mediaProperties.orphanGraceHours(), ChronoUnit.HOURS);
        var orphaned = imageRepository.findAllOrphaned(threshold);
        orphaned.forEach(this::deleteImageInternal);
        return orphaned.size();
    }

    private void deleteVideoInternal(Video v) {
        try {
            mediaStoragePort.deleteVideo(v.getExternalId());
        } catch (Exception e) {
            log.error("Failed to delete video from storage — externalId={}", v.getExternalId(), e);
        }
        videoRepository.deleteById(v.getId());
    }

    private void deleteImageInternal(Image i) {
        try {
            mediaStoragePort.deleteImage(i.getExternalId());
        } catch (Exception e) {
            log.error("Failed to delete image from storage — externalId={}", i.getExternalId(), e);
        }
        imageRepository.deleteById(i.getId());
    }

    public void deleteImage(Long imageId) {
        if (imageId == null) return;
        imageRepository.findById(imageId).ifPresent(this::deleteImageInternal);
    }
}
