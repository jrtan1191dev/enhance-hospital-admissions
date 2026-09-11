package com.hospital.admissions.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for logging patient self-service interactions from the tracking portal.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientActionRequest {
    private String actionType;
}
