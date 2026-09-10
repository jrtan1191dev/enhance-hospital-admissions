package com.hospital.admissions.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

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
