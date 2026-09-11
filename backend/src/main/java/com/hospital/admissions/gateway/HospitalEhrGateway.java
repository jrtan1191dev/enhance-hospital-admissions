package com.hospital.admissions.gateway;

import com.hospital.admissions.dto.PatientEhrSummary;

/**
 * Gateway contract for accessing Electronic Health Record (EHR) data from institutional hospital systems.
 */
public interface HospitalEhrGateway {

    /**
     * Fetches clinical EHR summary data (allergies, chronic conditions, labs, imaging) by patient NRIC.
     *
     * @param nric the masked or full National Registration Identity Card number.
     * @return {@link PatientEhrSummary} containing parsed clinical observations.
     */
    PatientEhrSummary fetchEhrSummary(String nric);
}
