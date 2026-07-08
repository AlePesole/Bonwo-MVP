package com.alessandropesole.bonwoapp.media.domain.port.out;

import com.alessandropesole.bonwoapp.media.domain.model.Image;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ImageRepository {
    Image save(Image image);

    Optional<Image> findById(Long id);

    Optional<Image> findByUploadToken(String token);

    List<Image> findAllById(Collection<Long> ids);

    List<Image> findAllExpiredPending();

    void deleteById(Long id);
}
