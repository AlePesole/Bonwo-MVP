package com.alessandropesole.bonwoapp.routine.infrastructure.persistence.mapper;

import com.alessandropesole.bonwoapp.routine.domain.model.ExerciseSlot;
import com.alessandropesole.bonwoapp.routine.domain.model.SetConfig;
import com.alessandropesole.bonwoapp.routine.infrastructure.persistence.entity.ExerciseSlotJpaEntity;
import com.alessandropesole.bonwoapp.routine.infrastructure.persistence.entity.RoutineJpaEntity;
import com.alessandropesole.bonwoapp.routine.infrastructure.persistence.entity.SetConfigEmbeddable;

import java.util.ArrayList;

public final class ExerciseSlotMapper {

    private ExerciseSlotMapper() {
    }

    public static ExerciseSlot toDomain(ExerciseSlotJpaEntity e) {
        var sets = e.getSets().stream().map(ExerciseSlotMapper::toDomain).toList();
        return ExerciseSlot.reconstitute(e.getExerciseId(), e.getPosition(), sets, e.getRestBetweenSets());
    }

    public static ExerciseSlotJpaEntity toEntity(ExerciseSlot slot, RoutineJpaEntity routine) {
        var sets = slot.getSets().stream().map(ExerciseSlotMapper::toEmbeddable).toList();
        return ExerciseSlotJpaEntity.builder()
                .routine(routine)
                .exerciseId(slot.getExerciseId())
                .position(slot.getPosition())
                .restBetweenSets(slot.getRestBetweenSets())
                .sets(new ArrayList<>(sets))
                .build();
    }

    private static SetConfig toDomain(SetConfigEmbeddable e) {
        return switch (e.getType()) {
            case REPS -> SetConfig.reps(e.getReps(), e.getWeightKg(), e.getWeightMode());
            case TIMED -> SetConfig.timed(e.getDuration(), e.getWeightKg(), e.getWeightMode());
            case AMRAP -> SetConfig.amrap(e.getDuration());
            case FAILURE -> SetConfig.toFailure(e.getWeightKg(), e.getWeightMode());
        };
    }

    private static SetConfigEmbeddable toEmbeddable(SetConfig s) {
        return new SetConfigEmbeddable(s.getType(), s.getReps(), s.getWeightKg(), s.getWeightMode(), s.getDuration());
    }
}
