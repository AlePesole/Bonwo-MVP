package com.alessandropesole.bonwoapp.media.domain.port.out;

import com.alessandropesole.bonwoapp.media.domain.model.Image;
import com.alessandropesole.bonwoapp.media.domain.model.Video;

/**
 * Output port — abstracts the external media storage provider.
 * <p>
 * The domain knows nothing about which provider is used.
 * The infrastructure adapter (e.g. CloudinaryMediaAdapter) implements this port.
 * <p>
 * UploadedVideo and UploadedImage are plain data carriers
 * returned by the provider after a successful upload.
 */
public interface MediaStoragePort {

    UploadedVideo uploadVideo(byte[] data, String filename, String mimeType, Long ownerId);

    UploadedImage uploadImage(byte[] data, String filename, String mimeType, Long ownerId);

    void deleteVideo(String externalId);

    void deleteImage(String externalId);

    record UploadedVideo(
            String externalId,
            String url,
            String thumbnailUrl,
            Integer durationSeconds
    ) {
    }

    record UploadedImage(
            String externalId,
            String url
    ) {
    }
}
