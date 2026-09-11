package com.hospital.admissions.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for modifying dynamic weights and batching thresholds used in BMU optimization.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BmuConfigUpdateRequest {
    private int weightSpecialtyCluster;
    private int weightConsolidation;
    private int weightFallRiskStation;
    private int batchHoldingWardThreshold;
}
