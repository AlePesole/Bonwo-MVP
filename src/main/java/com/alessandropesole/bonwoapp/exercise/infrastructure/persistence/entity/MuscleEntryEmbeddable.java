package com.alessandropesole.bonwoapp.exercise.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class MuscleEntryEmbeddable {

    @Column(name = "sub_group_id", nullable = false)
    private Long subGroupId;

    @Column(nullable = false)
    private Double activation;
}
