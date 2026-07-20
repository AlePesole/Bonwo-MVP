package com.alessandropesole.bonwoapp.exercise.domain.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class MuscleSummary {

    private final Map<Long, Double> scores;

    private MuscleSummary(Map<Long, Double> scores) {
        this.scores = Collections.unmodifiableMap(new HashMap<>(scores));
    }

    public static MuscleSummary of(Map<Long, Double> scores) {
        return new MuscleSummary(scores != null ? scores : Map.of());
    }

    public static MuscleSummary empty() {
        return new MuscleSummary(Map.of());
    }

    public Map<Long, Double> getScores() {
        return scores;
    }

    public double getScore(Long groupId) {
        return scores.getOrDefault(groupId, 0.0);
    }

    public boolean isEmpty() {
        return scores.isEmpty();
    }
}
