package com.alessandropesole.bonwoapp.exercise.application.service;

import com.alessandropesole.bonwoapp.catalog.domain.model.MuscleSubGroup;
import com.alessandropesole.bonwoapp.catalog.domain.port.out.MuscleSubGroupRepository;
import com.alessandropesole.bonwoapp.exercise.domain.model.MuscleEntry;
import com.alessandropesole.bonwoapp.exercise.domain.model.MuscleSummary;
import com.alessandropesole.bonwoapp.shared.infrastructure.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MuscleSummaryCalculatorTest {

    @Mock
    private MuscleSubGroupRepository muscleSubGroupRepository;

    @InjectMocks
    private MuscleSummaryCalculator calculator;

    @Test
    void calculate_returnsEmptyForNoMuscles() {
        MuscleSummary summary = calculator.calculate(List.of());

        assertThat(summary.isEmpty()).isTrue();
    }

    @Test
    void calculate_averagesActivationOfSubGroupsInTheSameGroup() {
        var upperChest = MuscleSubGroup.reconstitute(7L, 1L, "Upper Chest", null, "path", null, null);
        var lowerChest = MuscleSubGroup.reconstitute(3L, 1L, "Lower Chest", null, "path", null, null);
        when(muscleSubGroupRepository.findAllById(List.of(7L, 3L))).thenReturn(List.of(upperChest, lowerChest));
        List<MuscleEntry> muscles = List.of(MuscleEntry.of(7L, 0.7), MuscleEntry.of(3L, 0.6));

        MuscleSummary summary = calculator.calculate(muscles);

        assertThat(summary.getScore(1L)).isEqualTo(0.65);
    }

    @Test
    void calculate_keepsScoresIndependentAcrossDifferentGroups() {
        var chestSub = MuscleSubGroup.reconstitute(7L, 1L, "Upper Chest", null, "path", null, null);
        var backSub = MuscleSubGroup.reconstitute(11L, 4L, "Lower Back", null, null, "path", null);
        when(muscleSubGroupRepository.findAllById(List.of(7L, 11L))).thenReturn(List.of(chestSub, backSub));
        List<MuscleEntry> muscles = List.of(MuscleEntry.of(7L, 0.7), MuscleEntry.of(11L, 0.9));

        MuscleSummary summary = calculator.calculate(muscles);

        assertThat(summary.getScore(1L)).isEqualTo(0.7);
        assertThat(summary.getScore(4L)).isEqualTo(0.9);
    }

    @Test
    void calculate_throwsWhenSubGroupDoesNotExistInCatalog() {
        when(muscleSubGroupRepository.findAllById(List.of(99L))).thenReturn(List.of());
        List<MuscleEntry> muscles = List.of(MuscleEntry.of(99L, 0.5));

        assertThatThrownBy(() -> calculator.calculate(muscles))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void aggregate_sumsScoresAcrossSummaries() {
        MuscleSummary s1 = MuscleSummary.of(Map.of(1L, 0.5));
        MuscleSummary s2 = MuscleSummary.of(Map.of(1L, 0.3, 4L, 0.9));

        MuscleSummary aggregated = calculator.aggregate(List.of(s1, s2));

        assertThat(aggregated.getScore(1L)).isEqualTo(0.8);
        assertThat(aggregated.getScore(4L)).isEqualTo(0.9);
    }
}
