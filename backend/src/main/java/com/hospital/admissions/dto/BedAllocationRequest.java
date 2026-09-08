package com.hospital.admissions.dto;

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
public class BedAllocationRequest {
    @NotNull
    private UUID admissionRequestId;

    @NotNull
    private UUID bedId;

    private Integer rank;
    private Double score;
    private String overrideReason;

    public BedAllocationRequest(UUID admissionRequestId, UUID bedId) {
        this.admissionRequestId = admissionRequestId;
        this.bedId = bedId;
    }
}
