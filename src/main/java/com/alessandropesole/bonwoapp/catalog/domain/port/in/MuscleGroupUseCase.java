package com.alessandropesole.bonwoapp.catalog.domain.port.in;

import com.alessandropesole.bonwoapp.catalog.application.dto.MuscleGroupRequest;
import com.alessandropesole.bonwoapp.catalog.application.dto.MuscleGroupResponse;

import java.util.List;

public interface MuscleGroupUseCase {
    List<MuscleGroupResponse> listAll();

    MuscleGroupResponse create(MuscleGroupRequest request, Long adminId);

    MuscleGroupResponse update(Long id, MuscleGroupRequest request, Long adminId);

    void delete(Long id);
}
