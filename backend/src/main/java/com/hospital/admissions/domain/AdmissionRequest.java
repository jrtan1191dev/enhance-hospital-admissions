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
    private AcuityTier effectiveAcuityTier;

    private Boolean primaryTelemetry;
    private Boolean secondaryTelemetry;
    private Boolean effectiveTelemetry;

    private Boolean requiresSpecialistConsult;
    private Boolean reconciliationRequested;
    private Boolean clinicalConditionUpdated;

    @Enumerated(EnumType.STRING)
    private SpecialtyCluster admittingSpecialtyCluster;

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
    @Enumerated(EnumType.STRING)
    private DiversionPathway diversionPathway;
    private String sisterHospitalReferralId;

    private Boolean isRecommendationAccepted;

    @com.fasterxml.jackson.annotation.JsonProperty("discordant")
    private Boolean isDiscordant;

    private String overrideReasonCode;
    private String delayReasonTag;
    private String operationalDelayReason;
    private String archivedDelayReasonTag;
    private String archivedOperationalDelayReason;
    private LocalDateTime firstTrackerAccessedAt;

    private LocalDateTime referralDispatchedAt;
    @Builder.Default
    private Integer referralSlaMinutes = 30;
    private String referralFacility;
    private String virtualBedNumber;

    @Builder.Default
    private Boolean waitingInEd = true;

    @Version
    private Long version;
}
