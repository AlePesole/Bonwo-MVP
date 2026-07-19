package com.alessandropesole.bonwoapp.catalog.application.mapper;

import com.alessandropesole.bonwoapp.catalog.application.dto.MuscleGroupResponse;
import com.alessandropesole.bonwoapp.catalog.application.dto.MuscleSubGroupResponse;
import com.alessandropesole.bonwoapp.catalog.domain.model.MuscleGroup;
import com.alessandropesole.bonwoapp.media.application.dto.ImageResponse;

import java.util.List;

public final class MuscleGroupDtoMapper {

    private MuscleGroupDtoMapper() {
    }

    public static MuscleGroupResponse toResponse(MuscleGroup g,
                                                 ImageResponse icon,
                                                 List<MuscleSubGroupResponse> subGroups) {
        return new MuscleGroupResponse(g.getId(), g.getName(), icon, subGroups);
    }
}
