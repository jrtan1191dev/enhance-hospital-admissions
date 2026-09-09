package com.hospital.admissions.dto;

import com.hospital.admissions.domain.AcuityTier;
import com.hospital.admissions.domain.SpecialtyCluster;
import com.hospital.admissions.domain.WardClass;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

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

    public boolean isPrimaryTelemetry() {
        return primaryTelemetry || needsTelemetry;
    }

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
