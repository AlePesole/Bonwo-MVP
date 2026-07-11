package com.alessandropesole.bonwoapp.catalog.infrastructure.persistence.repository;

import com.alessandropesole.bonwoapp.catalog.domain.model.Activity;
import com.alessandropesole.bonwoapp.catalog.domain.port.out.ActivityRepository;
import com.alessandropesole.bonwoapp.catalog.infrastructure.persistence.mapper.ActivityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ActivityRepositoryAdapter implements ActivityRepository {

    private final ActivityJpaRepository activityJpa;

    @Override
    public Activity save(Activity a) {
        return ActivityMapper.toDomain(activityJpa.save(ActivityMapper.toEntity(a)));
    }

    @Override
    public Optional<Activity> findById(Long id) {
        return activityJpa.findById(id).map(ActivityMapper::toDomain);
    }

    @Override
    public List<Activity> findAll() {
        return activityJpa.findAll().stream().map(ActivityMapper::toDomain).toList();
    }

    @Override
    public List<Activity> findAllById(Set<Long> ids) {
        return activityJpa.findAllById(ids).stream().map(ActivityMapper::toDomain).toList();
    }

    @Override
    public boolean existsByName(String name) {
        return activityJpa.existsByName(name);
    }

    @Override
    public void deleteById(Long id) {
        activityJpa.deleteById(id);
    }
}
