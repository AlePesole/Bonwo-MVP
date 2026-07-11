package com.alessandropesole.bonwoapp.catalog.application.service;

import com.alessandropesole.bonwoapp.catalog.domain.port.out.ActivityRepository;
import com.alessandropesole.bonwoapp.catalog.domain.port.out.EquipmentRepository;
import com.alessandropesole.bonwoapp.catalog.domain.port.out.TrainingGoalRepository;
import com.alessandropesole.bonwoapp.shared.infrastructure.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class CatalogValidator {

    private final EquipmentRepository equipmentRepository;
    private final ActivityRepository activityRepository;
    private final TrainingGoalRepository trainingGoalRepository;

    public void validate(Set<Long> equipmentIds,
                         Set<Long> activityIds,
                         Set<Long> trainingGoalIds) {
        validateEquipment(equipmentIds);
        validateActivities(activityIds);
        validateTrainingGoals(trainingGoalIds);
    }

    public void validateEquipment(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        int found = equipmentRepository.findAllById(ids).size();
        if (found != ids.size()) {
            throw new ResourceNotFoundException(
                    "One or more equipmentIds do not exist in the catalog");
        }
    }

    public void validateActivities(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        int found = activityRepository.findAllById(ids).size();
        if (found != ids.size()) {
            throw new ResourceNotFoundException(
                    "One or more activityIds do not exist in the catalog");
        }
    }

    public void validateTrainingGoals(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        int found = trainingGoalRepository.findAllById(ids).size();
        if (found != ids.size()) {
            throw new ResourceNotFoundException(
                    "One or more trainingGoalIds do not exist in the catalog");
        }
    }
}
