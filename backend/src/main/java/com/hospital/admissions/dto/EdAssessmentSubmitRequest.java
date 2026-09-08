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
}
