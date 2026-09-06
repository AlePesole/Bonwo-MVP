package com.alessandropesole.bonwoapp.media.domain.port.out;

import com.alessandropesole.bonwoapp.media.domain.model.Video;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface VideoRepository {
    Video save(Video video);

    Optional<Video> findById(Long id);

    List<Video> findAllById(Collection<Long> ids);

    Optional<Video> findByUploadToken(String token);

    List<Video> findAllExpiredPending();

    void deleteById(Long id);
}
