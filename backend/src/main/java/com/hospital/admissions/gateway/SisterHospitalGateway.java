package com.hospital.admissions.gateway;

import com.hospital.admissions.entity.AdmissionRequest;
import com.hospital.admissions.entity.Patient;
import com.hospital.admissions.dto.SisterHospitalReferralResponse;

public interface SisterHospitalGateway {
    SisterHospitalReferralResponse referPatient(Patient patient, AdmissionRequest request, String targetFacility);
}
