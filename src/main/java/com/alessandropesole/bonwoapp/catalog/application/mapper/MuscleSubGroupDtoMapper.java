package com.alessandropesole.bonwoapp.catalog.application.mapper;

import com.alessandropesole.bonwoapp.catalog.application.dto.MuscleSubGroupResponse;
import com.alessandropesole.bonwoapp.catalog.domain.model.MuscleSubGroup;
import com.alessandropesole.bonwoapp.media.application.dto.ImageResponse;

public final class MuscleSubGroupDtoMapper {

    private MuscleSubGroupDtoMapper() {
    }

    public static MuscleSubGroupResponse toResponse(MuscleSubGroup s, ImageResponse icon) {
        return new MuscleSubGroupResponse(
                s.getId(), s.getGroupId(), s.getName(), s.getDetail(),
                s.getSvgPathFront(), s.getSvgPathBack(), icon);
    }
}
