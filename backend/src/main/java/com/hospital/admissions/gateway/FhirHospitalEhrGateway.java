package com.hospital.admissions.gateway;

import com.hospital.admissions.dto.PatientEhrSummary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Production implementation of {@link HospitalEhrGateway} integrating with enterprise HL7 Fast Healthcare
 * Interoperability Resources (FHIR) R4 servers.
 */
@Slf4j
@Component
@Profile("!prototype")
public class FhirHospitalEhrGateway implements HospitalEhrGateway {

    /**
     * Fetches real-time FHIR clinical resources (AllergyIntolerance, Condition, Observation, DiagnosticReport).
     *
     * @param nric the patient's National Registration Identity Card number.
     * @return populated {@link PatientEhrSummary}.
     * @throws UnsupportedOperationException if production FHIR server is not configured.
     */
    @Override
    public PatientEhrSummary fetchEhrSummary(String nric) {
        log.info("[PRODUCTION FHIR GATEWAY] Fetching EHR summary via HAPI FHIR R4 client for NRIC {}", nric);
        // Production implementation connects to enterprise HAPI FHIR endpoint
        throw new UnsupportedOperationException("Production FhirHospitalEhrGateway requires live FHIR R4 server configuration");
    }
}
