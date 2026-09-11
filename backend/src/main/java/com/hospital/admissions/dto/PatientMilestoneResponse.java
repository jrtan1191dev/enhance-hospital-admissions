package com.hospital.admissions.dto;

import com.hospital.admissions.entity.AdmissionStatus;
import com.hospital.admissions.entity.DischargeRunwayStage;
import com.hospital.admissions.entity.DiversionPathway;
import com.hospital.admissions.entity.EddConfidence;
import com.hospital.admissions.entity.MedicationDeliveryStatus;
import com.hospital.admissions.entity.WardClass;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Response payload returned to the patient tracker portal detailing the patient's queue position,
 * estimated wait time, assigned bed/ward milestones, and delay explanations.
 */
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
    private LocalDate estimatedDateOfDischarge;
    private EddConfidence eddConfidence;
    private MedicationDeliveryStatus medicationDeliveryStatus;
    private DischargeRunwayStage runwayStage;
}
