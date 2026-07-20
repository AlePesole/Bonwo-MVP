package com.alessandropesole.bonwoapp.exercise.application.service;

import com.alessandropesole.bonwoapp.catalog.domain.model.MuscleSubGroup;
import com.alessandropesole.bonwoapp.catalog.domain.port.out.MuscleSubGroupRepository;
import com.alessandropesole.bonwoapp.exercise.domain.model.MuscleEntry;
import com.alessandropesole.bonwoapp.exercise.domain.model.MuscleSummary;
import com.alessandropesole.bonwoapp.shared.infrastructure.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Validates and calculates MuscleSummary from a list of MuscleEntry.
 * Scores aggregated by MuscleGroup id:
 *   Each group's score is the average activation of its selected sub-groups.
 * muscleGroupIds are derived at response time from muscleSummary.getScores().keySet().
 */
@Component
@RequiredArgsConstructor
public class MuscleSummaryCalculator {

    private final MuscleSubGroupRepository muscleSubGroupRepository;

    public MuscleSummary calculate(List<MuscleEntry> muscles) {
        if (muscles == null || muscles.isEmpty())
            return MuscleSummary.empty();

        var subGroupIds = muscles.stream()
                .map(MuscleEntry::getSubGroupId)
                .distinct().toList();

        var groupIdBySubGroupId = muscleSubGroupRepository.findAllById(subGroupIds).stream()
                .collect(Collectors.toMap(MuscleSubGroup::getId, MuscleSubGroup::getGroupId));

        if (groupIdBySubGroupId.size() != subGroupIds.size())
            throw new ResourceNotFoundException(
                    "One or more muscle sub-group ids do not exist in the catalog");

        Map<Long, Double> scores = muscles.stream()
                .collect(Collectors.groupingBy(
                        entry -> groupIdBySubGroupId.get(entry.getSubGroupId()),
                        Collectors.averagingDouble(MuscleEntry::getActivation)))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> round(e.getValue())));
        return MuscleSummary.of(scores);
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    /** Aggregates multiple summaries — used for Routine and Program. */
    public MuscleSummary aggregate(List<MuscleSummary> summaries) {
        if (summaries == null || summaries.isEmpty()) return MuscleSummary.empty();
        Map<Long, Double> total = new HashMap<>();
        for (MuscleSummary s : summaries) {
            s.getScores().forEach((groupId, score) ->
                    total.merge(groupId, score, Double::sum));
        }
        return MuscleSummary.of(total);
    }
}
