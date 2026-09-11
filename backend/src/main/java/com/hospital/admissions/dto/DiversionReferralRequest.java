package com.hospital.admissions.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request payload for referring an acute patient to a partner sister hospital or community facility.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiversionReferralRequest {
    @NotNull
    private UUID admissionRequestId;

    @NotNull
    private String facility;
}
