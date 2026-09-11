package com.hospital.admissions.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing an inpatient hospital ward, including floor level, ward class,
 * specialty cluster alignment, negative pressure capability, and associated beds.
 */
@Entity
@Table(name = "wards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ward extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private int level;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WardClass wardClass;

    @Enumerated(EnumType.STRING)
    private Gender lockedGender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpecialtyCluster serviceCluster;

    @Column(nullable = false)
    private int capacity;

    @Column(nullable = false)
    @Builder.Default
    private boolean isNegativePressure = false;

    @Enumerated(EnumType.STRING)
    private InfectionStatus lockedInfectionStatus;

    @Column(nullable = false)
    @Builder.Default
    private boolean isHoldingWard = false;

    @OneToMany(mappedBy = "ward", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<Bed> beds = new ArrayList<>();
}
