package com.hospital.admissions.gateway;

import com.hospital.admissions.entity.AdmissionRequest;
import com.hospital.admissions.entity.Patient;
import com.hospital.admissions.dto.SisterHospitalReferralResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!prototype")
public class HttpSisterHospitalGateway implements SisterHospitalGateway {

    @Override
    public SisterHospitalReferralResponse referPatient(Patient patient, AdmissionRequest request, String targetFacility) {
        log.info("[PRODUCTION HTTP GATEWAY] Calling real external sister hospital API for patient {}", patient.getNricMasked());
        // Production implementation connects to real mTLS / OAuth2 REST endpoint
        throw new UnsupportedOperationException("Production SisterHospitalGateway requires external endpoint configuration");
    }
}
