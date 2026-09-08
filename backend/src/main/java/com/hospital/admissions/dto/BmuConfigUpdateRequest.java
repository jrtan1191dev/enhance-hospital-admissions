package com.hospital.admissions.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
