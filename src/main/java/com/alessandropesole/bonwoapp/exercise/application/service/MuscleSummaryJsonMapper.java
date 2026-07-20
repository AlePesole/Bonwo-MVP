package com.alessandropesole.bonwoapp.exercise.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.alessandropesole.bonwoapp.exercise.domain.model.MuscleSummary;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public final class MuscleSummaryJsonMapper {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<Long, Double>> TYPE = new TypeReference<>() {};

    private MuscleSummaryJsonMapper() {}

    public static String toJson(MuscleSummary summary) {
        if (summary == null || summary.isEmpty()) return null;
        try {
            return MAPPER.writeValueAsString(summary.getScores());
        } catch (Exception ex) {
            log.warn("Failed to serialize MuscleSummary to JSON", ex);
            return null;
        }
    }

    public static MuscleSummary fromJson(String json) {
        if (json == null || json.isBlank()) return MuscleSummary.empty();
        try {
            Map<Long, Double> scores = MAPPER.readValue(json, TYPE);
            return MuscleSummary.of(scores);
        } catch (Exception ex) {
            log.warn("Failed to deserialize MuscleSummary from JSON: {}", json, ex);
            return MuscleSummary.empty();
        }
    }
}
