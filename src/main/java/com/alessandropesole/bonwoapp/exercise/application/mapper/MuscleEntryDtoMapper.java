package com.alessandropesole.bonwoapp.exercise.application.mapper;

import com.alessandropesole.bonwoapp.catalog.application.dto.MuscleSubGroupResponse;
import com.alessandropesole.bonwoapp.exercise.application.dto.MuscleEntryDto;
import com.alessandropesole.bonwoapp.exercise.application.dto.MuscleEntryResponse;
import com.alessandropesole.bonwoapp.exercise.domain.model.MuscleEntry;

import java.util.List;

public final class MuscleEntryDtoMapper {

    private MuscleEntryDtoMapper() {
    }

    public static MuscleEntry toDomain(MuscleEntryDto dto) {
        return MuscleEntry.of(dto.subGroupId(), dto.activation());
    }

    public static List<MuscleEntry> toDomainList(List<MuscleEntryDto> dtos) {
        if (dtos == null) return List.of();
        return dtos.stream().map(MuscleEntryDtoMapper::toDomain).toList();
    }

    public static MuscleEntryResponse toResponse(MuscleEntry e, MuscleSubGroupResponse subGroup) {
        return new MuscleEntryResponse(
                e.getSubGroupId(), subGroup, e.getActivation(), e.role());
    }
}
