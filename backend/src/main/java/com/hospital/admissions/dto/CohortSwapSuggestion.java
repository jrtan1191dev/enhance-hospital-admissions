package com.hospital.admissions.dto;

import com.hospital.admissions.entity.Gender;
import com.hospital.admissions.entity.WardClass;
import lombok.*;

import java.util.UUID;

/**
 * Suggestion produced by BMU solvers to relocate an existing patient to another bed/ward,
 * unlocking multi-bed cohort rooms for waiting patients.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CohortSwapSuggestion {
    private String suggestionId;
    private UUID admissionRequestId;
    private UUID patientId;
    private String patientName;
    private Gender patientGender;
    private WardClass patientWardClass;
    private UUID currentBedId;
    private String currentBedNumber;
    private UUID currentWardId;
    private String currentWardName;
    private UUID targetBedId;
    private String targetBedNumber;
    private UUID targetWardId;
    private String targetWardName;
    private int unlockedCapacityCount;
}
