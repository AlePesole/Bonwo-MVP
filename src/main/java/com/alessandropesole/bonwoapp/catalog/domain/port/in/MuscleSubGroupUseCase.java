package com.alessandropesole.bonwoapp.catalog.domain.port.in;

import com.alessandropesole.bonwoapp.catalog.application.dto.CreateMuscleSubGroupRequest;
import com.alessandropesole.bonwoapp.catalog.application.dto.MuscleSubGroupResponse;
import com.alessandropesole.bonwoapp.catalog.application.dto.UpdateMuscleSubGroupRequest;

public interface MuscleSubGroupUseCase {
    MuscleSubGroupResponse create(CreateMuscleSubGroupRequest request, Long adminId);

    MuscleSubGroupResponse update(Long id, UpdateMuscleSubGroupRequest request, Long adminId);

    void delete(Long id);
}
