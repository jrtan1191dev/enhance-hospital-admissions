package com.hospital.admissions.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "admission_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdmissionRequest extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpecialtyCluster suspectedDiagnosisService;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AcuityTier primaryAcuityTier;

    @Enumerated(EnumType.STRING)
    private AcuityTier secondaryAcuityTier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WardClass requestedWardClass;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdmissionStatus status;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_bed_id")
    private Bed assignedBed;

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    private LocalDateTime allocatedAt;
    private LocalDateTime admittedAt;
    private LocalDateTime dischargedAt;

    private boolean diversionRecommended;
    private String sisterHospitalReferralId;

    private Boolean isRecommendationAccepted;

    @com.fasterxml.jackson.annotation.JsonProperty("discordant")
    private Boolean isDiscordant;

    private String overrideReasonCode;
    private String delayReasonTag;
    private LocalDateTime firstTrackerAccessedAt;
}
