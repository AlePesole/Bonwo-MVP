package com.alessandropesole.bonwoapp.catalog.domain.port.in;

import com.alessandropesole.bonwoapp.catalog.application.dto.ActivityRequest;
import com.alessandropesole.bonwoapp.catalog.application.dto.ActivityResponse;

import java.util.List;

public interface ActivityUseCase {
    List<ActivityResponse> listAll();

    ActivityResponse create(ActivityRequest request, Long adminId);

    ActivityResponse update(Long id, ActivityRequest request, Long adminId);

    void delete(Long id);
}
