package com.hospital.admissions.gateway;

import com.hospital.admissions.entity.AdmissionRequest;
import com.hospital.admissions.entity.Patient;
import com.hospital.admissions.dto.SisterHospitalReferralResponse;

/**
 * Gateway contract for transmitting subacute diversion referrals to external sister or community hospitals.
 */
public interface SisterHospitalGateway {

    /**
     * Transmits a fast-track subacute referral for a stabilized patient to an external healthcare facility.
     *
     * @param patient        the {@link Patient} being referred.
     * @param request        the active {@link AdmissionRequest}.
     * @param targetFacility name or identifier of the receiving hospital or facility.
     * @return {@link SisterHospitalReferralResponse} confirming receipt and SLA timer window.
     */
    SisterHospitalReferralResponse referPatient(Patient patient, AdmissionRequest request, String targetFacility);
}
