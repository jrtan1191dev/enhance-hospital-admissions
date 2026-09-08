package com.hospital.admissions.gateway;

import com.hospital.admissions.domain.AdmissionRequest;
import com.hospital.admissions.domain.BmuAlgorithmConfig;
import com.hospital.admissions.domain.Patient;
import com.hospital.admissions.dto.PatientEhrSummary;
import com.hospital.admissions.dto.SisterHospitalReferralResponse;
import com.hospital.admissions.solver.TimefoldBedAllocationSolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewaysAndSolversTest {

    @Test
    @DisplayName("MockHospitalEhrGateway returns synthetic EHR summary")
    void testMockHospitalEhrGateway() {
        MockHospitalEhrGateway gateway = new MockHospitalEhrGateway();
        PatientEhrSummary summary = gateway.fetchEhrSummary("S****123A");

        assertThat(summary).isNotNull();
        assertThat(summary.getNricMasked()).isEqualTo("S****123A");
        assertThat(summary.getAllergies()).contains("Penicillin");
        assertThat(summary.getChronicConditions()).contains("Hypertension");
    }

    @Test
    @DisplayName("MockSisterHospitalGateway generates referral response with target or default facility")
    void testMockSisterHospitalGateway() {
        MockSisterHospitalGateway gateway = new MockSisterHospitalGateway();
        Patient patient = Patient.builder().name("John Doe").nricMasked("S****999Z").build();
        AdmissionRequest request = AdmissionRequest.builder().build();

        SisterHospitalReferralResponse res1 = gateway.referPatient(patient, request, "OCH");
        assertThat(res1.getDestinationFacility()).isEqualTo("OCH");
        assertThat(res1.getReferralId()).startsWith("REF-OCH-");
        assertThat(res1.getSlaWindowMinutes()).isEqualTo(30);

        SisterHospitalReferralResponse res2 = gateway.referPatient(patient, request, null);
        assertThat(res2.getDestinationFacility()).isEqualTo("Outram Community Hospital (OCH)");
        assertThat(res2.getReferralId()).startsWith("REF-OCH-");
    }

    @Test
    @DisplayName("HttpSisterHospitalGateway throws UnsupportedOperationException in non-configured env")
    void testHttpSisterHospitalGateway() {
        HttpSisterHospitalGateway gateway = new HttpSisterHospitalGateway();
        Patient patient = Patient.builder().nricMasked("S****111A").build();

        assertThatThrownBy(() -> gateway.referPatient(patient, null, "AH"))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("Production SisterHospitalGateway requires external endpoint configuration");
    }

    @Test
    @DisplayName("FhirHospitalEhrGateway throws UnsupportedOperationException in non-configured env")
    void testFhirHospitalEhrGateway() {
        FhirHospitalEhrGateway gateway = new FhirHospitalEhrGateway();

        assertThatThrownBy(() -> gateway.fetchEhrSummary("S****111A"))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("Production FhirHospitalEhrGateway requires live FHIR R4 server configuration");
    }

    @Test
    @DisplayName("TimefoldBedAllocationSolver throws UnsupportedOperationException in non-configured env")
    void testTimefoldBedAllocationSolver() {
        TimefoldBedAllocationSolver solver = new TimefoldBedAllocationSolver();
        AdmissionRequest request = AdmissionRequest.builder().build();
        BmuAlgorithmConfig config = BmuAlgorithmConfig.builder().build();

        assertThatThrownBy(() -> solver.recommendBeds(request, config))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("Production TimefoldBedAllocationSolver requires active Timefold solver configuration");
    }
}
