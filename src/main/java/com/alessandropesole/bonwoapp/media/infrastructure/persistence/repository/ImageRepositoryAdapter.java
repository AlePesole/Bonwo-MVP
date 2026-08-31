package com.alessandropesole.bonwoapp.media.infrastructure.persistence.repository;

import com.alessandropesole.bonwoapp.media.domain.model.Image;
import com.alessandropesole.bonwoapp.media.domain.port.out.ImageRepository;
import com.alessandropesole.bonwoapp.media.infrastructure.persistence.mapper.MediaPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ImageRepositoryAdapter implements ImageRepository {
    private final ImageJpaRepository jpa;

    @Override
    public Image save(Image i) {
        return MediaPersistenceMapper.toDomain(jpa.save(MediaPersistenceMapper.toEntity(i)));
    }

    @Override
    public Optional<Image> findById(Long id) {
        return jpa.findById(id).map(MediaPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Image> findByUploadToken(String t) {
        return jpa.findByUploadToken(t).map(MediaPersistenceMapper::toDomain);
    }

    @Override
    public List<Image> findAllById(Collection<Long> ids) {
        return jpa.findAllById(ids).stream().map(MediaPersistenceMapper::toDomain).toList();
    }

    @Override
    public List<Image> findAllExpiredPending() {
        return jpa.findAllExpiredPending(Instant.now()).stream().map(MediaPersistenceMapper::toDomain).toList();
    }

    @Override
    public List<Image> findAllOrphaned(Instant createdBefore) {
        return jpa.findAllOrphaned(createdBefore).stream().map(MediaPersistenceMapper::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }
}
