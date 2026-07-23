package com.alessandropesole.bonwoapp.routine.infrastructure.persistence.entity;

import com.alessandropesole.bonwoapp.routine.domain.model.SetType;
import com.alessandropesole.bonwoapp.routine.domain.model.WeightMode;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class SetConfigEmbeddable {

    @Enumerated(EnumType.STRING)
    @Column(name = "set_type", nullable = false, length = 20)
    private SetType type;

    @Column(nullable = false)
    private int reps;

    @Column(name = "weight_kg")
    private Double weightKg;

    @Enumerated(EnumType.STRING)
    @Column(name = "weight_mode", length = 20)
    private WeightMode weightMode;

    private Duration duration;
}
