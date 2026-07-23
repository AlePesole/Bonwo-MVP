package com.alessandropesole.bonwoapp.routine.application.mapper;

import com.alessandropesole.bonwoapp.routine.application.dto.SetConfigDto;
import com.alessandropesole.bonwoapp.routine.application.dto.SetConfigResponse;
import com.alessandropesole.bonwoapp.routine.domain.model.SetConfig;

import java.util.List;

public final class SetConfigDtoMapper {

    private SetConfigDtoMapper() {
    }

    public static SetConfig toDomain(SetConfigDto dto) {
        return switch (dto.type()) {
            case REPS -> SetConfig.reps(dto.reps(), dto.weightKg(), dto.weightMode());
            case TIMED -> SetConfig.timed(dto.duration(), dto.weightKg(), dto.weightMode());
            case AMRAP -> SetConfig.amrap(dto.duration());
            case FAILURE -> SetConfig.toFailure(dto.weightKg(), dto.weightMode());
        };
    }

    public static List<SetConfig> toDomainList(List<SetConfigDto> dtos) {
        if (dtos == null) return List.of();
        return dtos.stream().map(SetConfigDtoMapper::toDomain).toList();
    }

    public static SetConfigResponse toResponse(SetConfig s) {
        return new SetConfigResponse(
                s.getType(), s.getReps(), s.getWeightKg(), s.getWeightMode(),
                s.totalWeightKg(), s.getDuration());
    }
}
