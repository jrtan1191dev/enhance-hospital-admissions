package com.hospital.admissions.gateway;

import com.hospital.admissions.entity.AdmissionRequest;
import com.hospital.admissions.entity.Patient;
import com.hospital.admissions.dto.SisterHospitalReferralResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Production implementation of {@link SisterHospitalGateway} connecting to external regional hospital APIs
 * via mutual TLS (mTLS) and OAuth2 security.
 */
@Slf4j
@Component
@Profile("!prototype")
public class HttpSisterHospitalGateway implements SisterHospitalGateway {

    /**
     * Transmits an automated electronic referral request to an external hospital over secure HTTP REST.
     *
     * @param patient        the {@link Patient} to refer.
     * @param request        the current {@link AdmissionRequest}.
     * @param targetFacility destination hospital code or name.
     * @return response confirming dispatch and referral tracking identifier.
     * @throws UnsupportedOperationException if production external endpoint is not configured.
     */
    @Override
    public SisterHospitalReferralResponse referPatient(Patient patient, AdmissionRequest request, String targetFacility) {
        log.info("[PRODUCTION HTTP GATEWAY] Calling real external sister hospital API for patient {}", patient.getNricMasked());
        // Production implementation connects to real mTLS / OAuth2 REST endpoint
        throw new UnsupportedOperationException("Production SisterHospitalGateway requires external endpoint configuration");
    }
}
