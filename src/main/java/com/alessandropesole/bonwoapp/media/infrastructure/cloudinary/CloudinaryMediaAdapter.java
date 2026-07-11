package com.alessandropesole.bonwoapp.media.infrastructure.cloudinary;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.alessandropesole.bonwoapp.media.domain.port.out.MediaStoragePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CloudinaryMediaAdapter implements MediaStoragePort {

    private final Cloudinary cloudinary;

    @Override
    public UploadedVideo uploadVideo(byte[] data, String filename,
                                     String mimeType, Long ownerId) {
        try {
            Map<?, ?> result = cloudinary.uploader().upload(data, ObjectUtils.asMap(
                    "resource_type", "video",
                    "folder", "bonwoapp/videos/" + ownerId,
                    "eager", "so_0",            // generate thumbnail at second 0
                    "eager_async", false
            ));

            String externalId = (String) result.get("public_id");
            String url = (String) result.get("secure_url");
            Integer duration = result.get("duration") != null
                    ? ((Number) result.get("duration")).intValue() : null;

            // Thumbnail URL: Cloudinary eager transformation
            String thumbnailUrl = null;
            if (result.get("eager") instanceof java.util.List<?> eager && !eager.isEmpty()) {
                thumbnailUrl = (String) ((Map<?, ?>) eager.get(0)).get("secure_url");
            }

            log.info("Video uploaded to Cloudinary: externalId={}", externalId);
            return new UploadedVideo(externalId, url, thumbnailUrl, duration);

        } catch (IOException e) {
            throw new RuntimeException("Cloudinary video upload failed", e);
        }
    }

    @Override
    public UploadedImage uploadImage(byte[] data, String filename,
                                     String mimeType, Long ownerId) {
        try {
            Map<?, ?> result = cloudinary.uploader().upload(data, ObjectUtils.asMap(
                    "resource_type", "image",
                    "folder", "bonwoapp/images/" + ownerId
            ));

            String externalId = (String) result.get("public_id");
            String url = (String) result.get("secure_url");

            log.info("Image uploaded to Cloudinary: externalId={}", externalId);
            return new UploadedImage(externalId, url);

        } catch (IOException e) {
            throw new RuntimeException("Cloudinary image upload failed", e);
        }
    }

    @Override
    public void deleteVideo(String externalId) {
        try {
            cloudinary.uploader().destroy(externalId,
                    ObjectUtils.asMap("resource_type", "video"));
            log.info("Video deleted from Cloudinary: externalId={}", externalId);
        } catch (IOException e) {
            log.error("Failed to delete video from Cloudinary: externalId={}", externalId, e);
        }
    }

    @Override
    public void deleteImage(String externalId) {
        try {
            cloudinary.uploader().destroy(externalId,
                    ObjectUtils.asMap("resource_type", "image"));
            log.info("Image deleted from Cloudinary: externalId={}", externalId);
        } catch (IOException e) {
            log.error("Failed to delete image from Cloudinary: externalId={}", externalId, e);
        }
    }
}
