package com.hospital.admissions.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchApprovalRequest {
    private String suggestionId;
    private UUID targetWardId;
    private List<UUID> admissionRequestIds;
}
