package com.hospital.admissions.dto;

import com.hospital.admissions.entity.DischargeRunwayStage;
import com.hospital.admissions.entity.EddConfidence;
import com.hospital.admissions.entity.MedicationDeliveryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO representing an inpatient on the discharge runway.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DischargeRunwayDto {
    private UUID patientId;
    private String patientName;
    private UUID bedId;
    private String bedNumber;
    private String wardCode;
    private Integer levelNumber;
    private LocalDate estimatedDateOfDischarge;
    private EddConfidence confidence;
    private DischargeRunwayStage runwayStage;
    private LocalDateTime dischargeSignoffAt;
    private MedicationDeliveryStatus medicationStatus;
    private String rationale;
}
