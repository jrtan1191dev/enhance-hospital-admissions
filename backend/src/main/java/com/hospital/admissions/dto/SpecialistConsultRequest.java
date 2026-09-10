package com.hospital.admissions.dto;

import com.hospital.admissions.entity.AcuityTier;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpecialistConsultRequest {
    @NotNull
    private AcuityTier secondaryAcuityTier;

    private Boolean secondaryTelemetry;
    private String consultNotes;
    private boolean diversionRecommended;
    private com.hospital.admissions.entity.DiversionPathway diversionPathway;

    public SpecialistConsultRequest(AcuityTier secondaryAcuityTier, String consultNotes, boolean diversionRecommended) {
        this.secondaryAcuityTier = secondaryAcuityTier;
        this.consultNotes = consultNotes;
        this.diversionRecommended = diversionRecommended;
    }
}
