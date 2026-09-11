package com.hospital.admissions.dto;

import com.hospital.admissions.entity.AcuityTier;
import com.hospital.admissions.entity.SpecialtyCluster;
import com.hospital.admissions.entity.WardClass;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request payload submitted by emergency department clinicians capturing triage findings,
 * acuity tier, required service cluster, telemetry needs, and specialist consult requirements.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EdAssessmentSubmitRequest {
    @NotNull
    private UUID patientId;

    @NotNull
    private SpecialtyCluster suspectedDiagnosisService;

    @NotNull
    private AcuityTier primaryAcuityTier;

    @NotNull
    private WardClass requestedWardClass;

    private boolean needsTelemetry;
    private boolean primaryTelemetry;
    private boolean requiresSpecialistConsult;
    private java.util.Set<SpecialtyCluster> targetClusters;
    private java.util.List<ClinicalBaselineOverride> overrides;
    private String clinicalNotes;
    private Boolean recommendedAccepted;
    private Double elapsedMins;

    /**
     * Resolves whether telemetry is indicated, checking both primaryTelemetry and needsTelemetry flags.
     *
     * @return true if telemetry is needed
     */
    public boolean isPrimaryTelemetry() {
        return primaryTelemetry || needsTelemetry;
    }

    /**
     * Convenience constructor for initializing baseline emergency assessments.
     *
     * @param patientId                  patient unique identifier
     * @param suspectedDiagnosisService  suspected primary diagnosis service
     * @param primaryAcuityTier          assigned triage acuity tier
     * @param requestedWardClass         requested hospital ward class
     * @param needsTelemetry             whether telemetry cardiac monitoring is required
     */
    public EdAssessmentSubmitRequest(UUID patientId, SpecialtyCluster suspectedDiagnosisService, AcuityTier primaryAcuityTier, WardClass requestedWardClass, boolean needsTelemetry) {
        this.patientId = patientId;
        this.suspectedDiagnosisService = suspectedDiagnosisService;
        this.primaryAcuityTier = primaryAcuityTier;
        this.requestedWardClass = requestedWardClass;
        this.needsTelemetry = needsTelemetry;
        this.primaryTelemetry = needsTelemetry;
        this.recommendedAccepted = true;
    }
}
