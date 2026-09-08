package com.hospital.admissions.gateway;

import com.hospital.admissions.dto.PatientEhrSummary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("prototype")
public class MockHospitalEhrGateway implements HospitalEhrGateway {

    @Override
    public PatientEhrSummary fetchEhrSummary(String nric) {
        log.info("[PROTOTYPE MOCK] Fetching synthetic EHR summary for NRIC {}", nric);
        return PatientEhrSummary.builder()
                .nricMasked(nric)
                .allergies("Penicillin (Mild rash)")
                .chronicConditions("Hypertension, Type 2 Diabetes Mellitus, Dyslipidemia")
                .recentLabs("HbA1c 7.2%, eGFR 68 mL/min, Troponin-T < 14 ng/L (Baseline)")
                .imagingSummary("CXR (ED Intake): No acute consolidation, cardiomegaly noted.")
                .build();
    }
}
