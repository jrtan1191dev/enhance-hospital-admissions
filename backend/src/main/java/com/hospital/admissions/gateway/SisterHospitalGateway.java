package com.hospital.admissions.gateway;

import com.hospital.admissions.domain.AdmissionRequest;
import com.hospital.admissions.domain.Patient;
import com.hospital.admissions.dto.SisterHospitalReferralResponse;

public interface SisterHospitalGateway {
    SisterHospitalReferralResponse referPatient(Patient patient, AdmissionRequest request, String targetFacility);
}
