package com.alessandropesole.bonwoapp.media.infrastructure.persistence.repository;

import com.alessandropesole.bonwoapp.media.domain.model.Video;
import com.alessandropesole.bonwoapp.media.domain.port.out.VideoRepository;
import com.alessandropesole.bonwoapp.media.infrastructure.persistence.mapper.MediaPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class VideoRepositoryAdapter implements VideoRepository {
    private final VideoJpaRepository jpa;

    @Override
    public Video save(Video v) {
        return MediaPersistenceMapper.toDomain(jpa.save(MediaPersistenceMapper.toEntity(v)));
    }

    @Override
    public Optional<Video> findById(Long id) {
        return jpa.findById(id).map(MediaPersistenceMapper::toDomain);
    }

    @Override
    public List<Video> findAllById(Collection<Long> ids) {
        return jpa.findAllById(ids).stream().map(MediaPersistenceMapper::toDomain).toList();
    }

    @Override
    public Optional<Video> findByUploadToken(String t) {
        return jpa.findByUploadToken(t).map(MediaPersistenceMapper::toDomain);
    }

    @Override
    public List<Video> findAllExpiredPending() {
        return jpa.findAllExpiredPending(Instant.now()).stream().map(MediaPersistenceMapper::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }
}
