package com.hospital.admissions.gateway;

import com.hospital.admissions.entity.AdmissionRequest;
import com.hospital.admissions.entity.Patient;
import com.hospital.admissions.dto.SisterHospitalReferralResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Prototype mock implementation of {@link SisterHospitalGateway} returning simulated fast-track referral confirmations.
 */
@Slf4j
@Component
@Profile("prototype")
public class MockSisterHospitalGateway implements SisterHospitalGateway {

    /**
     * Generates a simulated referral confirmation code and 30-minute SLA window.
     *
     * @param patient        the {@link Patient} being diverted.
     * @param request        the current {@link AdmissionRequest}.
     * @param targetFacility destination hospital facility name.
     * @return synthetic {@link SisterHospitalReferralResponse}.
     */
    @Override
    public SisterHospitalReferralResponse referPatient(Patient patient, AdmissionRequest request, String targetFacility) {
        String referralId = "REF-" + (targetFacility != null ? targetFacility.toUpperCase() : "OCH") + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("[PROTOTYPE MOCK] Dispatched diversion referral {} for patient {} ({}) to {}",
                referralId, patient.getName(), patient.getNricMasked(), targetFacility);
        return SisterHospitalReferralResponse.builder()
                .referralId(referralId)
                .destinationFacility(targetFacility != null ? targetFacility : "Outram Community Hospital (OCH)")
                .status("ACCEPTED_30MIN_SLA")
                .slaWindowMinutes(30)
                .notes("Simulated fast-track subacute bed reservation confirmed. Ambulance dispatch notification queued.")
                .build();
    }
}
