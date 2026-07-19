package com.alessandropesole.bonwoapp.catalog.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "muscle_sub_groups", indexes = {
        @Index(name = "idx_muscle_sub_group_group_name", columnList = "group_id, name", unique = true)
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MuscleSubGroupJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String detail;

    @Column(name = "svg_path_front", columnDefinition = "TEXT")
    private String svgPathFront;

    @Column(name = "svg_path_back", columnDefinition = "TEXT")
    private String svgPathBack;

    @Column(name = "icon_id")
    private Long iconId;
}
