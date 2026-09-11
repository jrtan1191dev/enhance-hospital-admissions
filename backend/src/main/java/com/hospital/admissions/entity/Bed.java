package com.hospital.admissions.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing an individual hospital bed within a ward, including its telemetry capabilities,
 * proximity to the nursing station, cleaning status, and current occupancy.
 */
@Entity
@Table(name = "beds")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bed extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ward_id", nullable = false)
    @JsonIgnoreProperties("beds")
    private Ward ward;

    @Column(nullable = false)
    private String bedNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BedStatus status;

    @Column(nullable = false)
    private boolean isNearNursingStation;

    @Column(nullable = false)
    private boolean hasTelemetry;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "current_patient_id")
    private Patient currentPatient;

    private LocalDateTime cleaningStartedAt;
    private LocalDateTime lastCleanedAt;

    @Version
    private Long version;
}
