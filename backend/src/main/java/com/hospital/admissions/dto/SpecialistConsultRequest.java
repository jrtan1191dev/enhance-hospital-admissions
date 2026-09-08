package com.hospital.admissions.dto;

import com.hospital.admissions.domain.AcuityTier;
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

    private String consultNotes;
    private boolean diversionRecommended;
}
