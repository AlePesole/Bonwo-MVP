package com.alessandropesole.bonwoapp.session.application.mapper;

import com.alessandropesole.bonwoapp.session.application.dto.TrainingSetDto;
import com.alessandropesole.bonwoapp.session.application.dto.TrainingSetResponse;
import com.alessandropesole.bonwoapp.session.domain.model.TrainingSet;

import java.util.List;

public final class TrainingSetDtoMapper {

    private TrainingSetDtoMapper() {
    }

    public static TrainingSet toDomain(TrainingSetDto dto) {
        return switch (dto.type()) {
            case REPS -> TrainingSet.reps(dto.reps(), dto.weightKg(), dto.weightMode(), dto.done());
            case TIMED -> TrainingSet.timed(dto.duration(), dto.weightKg(), dto.weightMode(), dto.done());
            case AMRAP -> TrainingSet.amrap(dto.duration(), dto.done());
            case FAILURE -> TrainingSet.toFailure(dto.weightKg(), dto.weightMode(), dto.done());
        };
    }

    public static List<TrainingSet> toDomainList(List<TrainingSetDto> dtos) {
        if (dtos == null) return List.of();
        return dtos.stream().map(TrainingSetDtoMapper::toDomain).toList();
    }

    public static TrainingSetResponse toResponse(TrainingSet s) {
        return new TrainingSetResponse(
                s.getType(), s.getReps(), s.getWeightKg(), s.getWeightMode(),
                s.totalWeightKg(), s.getDuration(), s.isDone());
    }
}
