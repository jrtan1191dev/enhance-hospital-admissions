package com.hospital.admissions.dto;

import com.hospital.admissions.domain.AdmissionStatus;
import com.hospital.admissions.domain.DiversionPathway;
import com.hospital.admissions.domain.WardClass;
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
    private WardClass requestedWardClass;
    private int queuePosition;
    private int patientsAhead;
    private int estimatedWaitMinutes;
    private String assignedBedNumber;
    private String assignedWardName;
    private Integer assignedLevel;
    private String delayReason;
    private String delayContactHotline;
    private String coPayEstimate;
    private String careGuidance;
    private Boolean diversionRecommended;
    private DiversionPathway diversionPathway;
    private java.time.LocalDate estimatedDateOfDischarge;
    private com.hospital.admissions.domain.EddConfidence eddConfidence;
    private com.hospital.admissions.domain.MedicationDeliveryStatus medicationDeliveryStatus;
    private com.hospital.admissions.domain.DischargeRunwayStage runwayStage;
}
