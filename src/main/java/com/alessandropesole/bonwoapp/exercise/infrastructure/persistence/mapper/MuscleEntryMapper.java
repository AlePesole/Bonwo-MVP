package com.alessandropesole.bonwoapp.exercise.infrastructure.persistence.mapper;

import com.alessandropesole.bonwoapp.exercise.domain.model.MuscleEntry;
import com.alessandropesole.bonwoapp.exercise.infrastructure.persistence.entity.MuscleEntryEmbeddable;

import java.util.ArrayList;
import java.util.List;

public final class MuscleEntryMapper {

    private MuscleEntryMapper() {
    }

    public static MuscleEntry toDomain(MuscleEntryEmbeddable e) {
        return MuscleEntry.of(e.getSubGroupId(), e.getActivation());
    }

    public static MuscleEntryEmbeddable toEmbeddable(MuscleEntry m) {
        return new MuscleEntryEmbeddable(m.getSubGroupId(), m.getActivation());
    }

    public static List<MuscleEntry> toDomainList(List<MuscleEntryEmbeddable> list) {
        if (list == null) return List.of();
        return list.stream().map(MuscleEntryMapper::toDomain).toList();
    }

    public static List<MuscleEntryEmbeddable> toEmbeddableList(List<MuscleEntry> list) {
        if (list == null) return new ArrayList<>();
        return new ArrayList<>(list.stream().map(MuscleEntryMapper::toEmbeddable).toList());
    }
}
