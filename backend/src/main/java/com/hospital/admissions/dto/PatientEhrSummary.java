package com.hospital.admissions.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
