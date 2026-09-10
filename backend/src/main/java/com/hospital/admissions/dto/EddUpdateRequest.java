package com.hospital.admissions.dto;

import com.hospital.admissions.entity.EddConfidence;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Payload for setting or updating Estimated Date of Discharge (EDD).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EddUpdateRequest {

    @NotNull(message = "EDD is required")
    private LocalDate edd;

    @NotNull(message = "Confidence rating is required")
    private EddConfidence eddConfidence;

    private String rationale;
}
