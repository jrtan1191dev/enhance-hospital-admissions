package com.hospital.admissions.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a clinician override of automated triage baseline data with audit rationale.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicalBaselineOverride {
    private String field;
    private String originalValue;
    private String submittedValue;
    private String overrideReason;
}
