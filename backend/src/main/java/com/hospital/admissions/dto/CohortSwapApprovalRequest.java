package com.hospital.admissions.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

/**
 * Request payload for approving a suggested cohort swap of an inpatient to release a locked bed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CohortSwapApprovalRequest {
    @NotNull
    private UUID admissionRequestId;

    @NotNull
    private UUID targetBedId;
}
