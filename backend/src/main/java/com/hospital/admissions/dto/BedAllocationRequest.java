package com.hospital.admissions.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request payload for allocating a specific bed to an admission request,
 * optionally including rank, score, and clinical/operational override reasons.
 */
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

    /**
     * Constructs a basic bed allocation request with required identifiers.
     *
     * @param admissionRequestId unique identifier of the admission request
     * @param bedId              unique identifier of the target bed
     */
    public BedAllocationRequest(UUID admissionRequestId, UUID bedId) {
        this.admissionRequestId = admissionRequestId;
        this.bedId = bedId;
    }
}
