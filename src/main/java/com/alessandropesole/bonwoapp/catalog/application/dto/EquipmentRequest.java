package com.alessandropesole.bonwoapp.catalog.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EquipmentRequest(
        @NotBlank @Size(max = 100) String name,
        Long iconId
) {}
