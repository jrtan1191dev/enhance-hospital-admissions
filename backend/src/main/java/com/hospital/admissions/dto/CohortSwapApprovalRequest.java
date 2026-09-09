package com.hospital.admissions.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

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
