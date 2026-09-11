package com.hospital.admissions.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

/**
 * Request payload for approving a batch allocation of cohort-compatible patients
 * into an available holding ward.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchApprovalRequest {
    private String suggestionId;
    private UUID targetWardId;
    private List<UUID> admissionRequestIds;
}
