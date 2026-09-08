package com.hospital.admissions.gateway;

import com.hospital.admissions.dto.PatientEhrSummary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!prototype")
public class FhirHospitalEhrGateway implements HospitalEhrGateway {

    @Override
    public PatientEhrSummary fetchEhrSummary(String nric) {
        log.info("[PRODUCTION FHIR GATEWAY] Fetching EHR summary via HAPI FHIR R4 client for NRIC {}", nric);
        // Production implementation connects to enterprise HAPI FHIR endpoint
        throw new UnsupportedOperationException("Production FhirHospitalEhrGateway requires live FHIR R4 server configuration");
    }
}
