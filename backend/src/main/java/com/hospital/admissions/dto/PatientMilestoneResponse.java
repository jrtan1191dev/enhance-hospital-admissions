package com.hospital.admissions.dto;

import com.hospital.admissions.domain.AdmissionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientMilestoneResponse {
    private UUID patientId;
    private String patientName;
    private String queueToken;
    private AdmissionStatus admissionStatus;
    private int queuePosition;
    private int estimatedWaitMinutes;
    private String assignedBedNumber;
    private String assignedWardName;
    private Integer assignedLevel;
    private String coPayEstimate;
    private String careGuidance;
}
