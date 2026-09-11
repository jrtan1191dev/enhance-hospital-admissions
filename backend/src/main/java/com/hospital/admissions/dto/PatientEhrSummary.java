package com.hospital.admissions.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Summary DTO containing patient clinical baseline information from the Electronic Health Record (EHR).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientEhrSummary {
    private String nricMasked;
    private String allergies;
    private String chronicConditions;
    private String recentLabs;
    private String imagingSummary;
}
