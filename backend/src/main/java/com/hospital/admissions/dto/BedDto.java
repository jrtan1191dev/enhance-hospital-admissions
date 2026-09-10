package com.hospital.admissions.dto;

import com.hospital.admissions.entity.BedStatus;
import com.hospital.admissions.entity.Patient;

import java.util.UUID;

public record BedDto(
        UUID id,
        String bedNumber,
        BedStatus status,
        boolean telemetryCapable,
        boolean nearNursingStation,
        Patient assignedPatient
) {}
