package com.alessandropesole.bonwoapp.session.infrastructure.persistence.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.alessandropesole.bonwoapp.session.domain.model.TrainingSet;
import com.alessandropesole.bonwoapp.session.domain.model.TrainingSlot;
import com.alessandropesole.bonwoapp.session.infrastructure.persistence.json.TrainingSetJson;
import com.alessandropesole.bonwoapp.session.infrastructure.persistence.json.TrainingSlotJson;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public final class TrainingSlotJsonMapper {

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());
    private static final TypeReference<List<TrainingSlotJson>> TYPE = new TypeReference<>() {};

    private TrainingSlotJsonMapper() {
    }

    public static String toJson(List<TrainingSlot> slots) {
        try {
            List<TrainingSlotJson> json = slots.stream().map(TrainingSlotJsonMapper::toJson).toList();
            return MAPPER.writeValueAsString(json);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize TrainingSession slots to JSON", ex);
        }
    }

    public static List<TrainingSlot> fromJson(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            List<TrainingSlotJson> parsed = MAPPER.readValue(json, TYPE);
            return parsed.stream().map(TrainingSlotJsonMapper::toDomain).toList();
        } catch (Exception ex) {
            log.error("Failed to deserialize TrainingSession slots from JSON: {}", json, ex);
            throw new IllegalStateException("Failed to deserialize TrainingSession slots", ex);
        }
    }

    private static TrainingSlotJson toJson(TrainingSlot slot) {
        var sets = slot.getSets().stream().map(TrainingSlotJsonMapper::toJson).toList();
        return new TrainingSlotJson(slot.getExerciseId(), slot.getPosition(), sets, slot.getRestBetweenSets());
    }

    private static TrainingSetJson toJson(TrainingSet s) {
        return new TrainingSetJson(s.getType(), s.getReps(), s.getWeightKg(), s.getWeightMode(), s.getDuration(), s.isDone());
    }

    private static TrainingSlot toDomain(TrainingSlotJson dto) {
        var sets = dto.sets().stream().map(TrainingSlotJsonMapper::toDomain).toList();
        return TrainingSlot.reconstitute(dto.exerciseId(), dto.position(), sets, dto.restBetweenSets());
    }

    private static TrainingSet toDomain(TrainingSetJson dto) {
        return switch (dto.type()) {
            case REPS -> TrainingSet.reps(dto.reps(), dto.weightKg(), dto.weightMode(), dto.done());
            case TIMED -> TrainingSet.timed(dto.duration(), dto.weightKg(), dto.weightMode(), dto.done());
            case AMRAP -> TrainingSet.amrap(dto.duration(), dto.done());
            case FAILURE -> TrainingSet.toFailure(dto.weightKg(), dto.weightMode(), dto.done());
        };
    }
}
