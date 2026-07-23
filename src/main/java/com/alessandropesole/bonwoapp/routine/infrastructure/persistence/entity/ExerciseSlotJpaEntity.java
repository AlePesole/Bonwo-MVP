package com.alessandropesole.bonwoapp.routine.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "routine_slots")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExerciseSlotJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routine_id", nullable = false)
    private RoutineJpaEntity routine;

    @Column(name = "exercise_id", nullable = false)
    private Long exerciseId;

    @Column(nullable = false)
    private int position;

    @Column(name = "rest_between_sets")
    private Duration restBetweenSets;

    @ElementCollection
    @CollectionTable(name = "routine_slot_sets",
            joinColumns = @JoinColumn(name = "slot_id"))
    @OrderColumn(name = "set_order")
    @Builder.Default
    private List<SetConfigEmbeddable> sets = new ArrayList<>();
}
