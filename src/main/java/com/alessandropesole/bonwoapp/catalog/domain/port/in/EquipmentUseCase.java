package com.alessandropesole.bonwoapp.catalog.domain.port.in;

import com.alessandropesole.bonwoapp.catalog.application.dto.EquipmentRequest;
import com.alessandropesole.bonwoapp.catalog.application.dto.EquipmentResponse;

import java.util.List;

public interface EquipmentUseCase {
    List<EquipmentResponse> listAll();

    EquipmentResponse create(EquipmentRequest request, Long adminId);

    EquipmentResponse update(Long id, EquipmentRequest request, Long adminId);

    void delete(Long id);
}
