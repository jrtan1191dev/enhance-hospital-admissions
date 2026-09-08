package com.hospital.admissions.gateway;

import com.hospital.admissions.dto.PatientEhrSummary;

public interface HospitalEhrGateway {
    PatientEhrSummary fetchEhrSummary(String nric);
}
