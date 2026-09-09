package com.hospital.admissions.dto;

import com.hospital.admissions.domain.BedStatus;
import com.hospital.admissions.domain.Patient;

import java.util.UUID;

public record BedDto(
        UUID id,
        String bedNumber,
        BedStatus status,
        boolean telemetryCapable,
        boolean nearNursingStation,
        Patient assignedPatient
) {}
