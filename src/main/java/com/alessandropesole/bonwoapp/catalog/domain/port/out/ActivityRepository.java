package com.alessandropesole.bonwoapp.catalog.domain.port.out;

import com.alessandropesole.bonwoapp.catalog.domain.model.Activity;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ActivityRepository {
    Activity save(Activity activity);

    Optional<Activity> findById(Long id);

    List<Activity> findAll();

    List<Activity> findAllById(Set<Long> ids);

    boolean existsByName(String name);

    void deleteById(Long id);
}
