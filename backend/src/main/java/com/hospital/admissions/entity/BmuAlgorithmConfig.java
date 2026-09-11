package com.hospital.admissions.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Configuration entity controlling the weights and thresholds used by the BMU optimization
 * and bed allocation solvers.
 */
@Entity
@Table(name = "bmu_algorithm_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BmuAlgorithmConfig extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private int weightSpecialtyCluster;

    @Column(nullable = false)
    private int weightConsolidation;

    @Column(nullable = false)
    private int weightFallRiskStation;

    @Column(nullable = false)
    private int batchHoldingWardThreshold;
}
