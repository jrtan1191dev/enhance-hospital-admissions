package com.hospital.admissions.dto;

import com.hospital.admissions.entity.BedStatus;
import com.hospital.admissions.entity.Patient;

import java.util.UUID;

/**
 * Data transfer record representing a bed with telemetry capabilities and occupancy information.
 *
 * @param id                  bed unique identifier
 * @param bedNumber           physical bed identifier within the ward
 * @param status              current bed status
 * @param telemetryCapable    whether telemetry monitoring is installed
 * @param nearNursingStation  whether bed is directly adjacent to nursing station
 * @param assignedPatient     patient currently assigned or occupying the bed
 */
public record BedDto(
        UUID id,
        String bedNumber,
        BedStatus status,
        boolean telemetryCapable,
        boolean nearNursingStation,
        Patient assignedPatient
) {}
