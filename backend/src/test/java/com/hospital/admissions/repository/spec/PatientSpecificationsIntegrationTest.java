package com.hospital.admissions.repository.spec;

import com.hospital.admissions.entity.AcuityTier;
import com.hospital.admissions.entity.AdmissionRequest;
import com.hospital.admissions.entity.AdmissionStatus;
import com.hospital.admissions.entity.Gender;
import com.hospital.admissions.entity.InfectionStatus;
import com.hospital.admissions.entity.Patient;
import com.hospital.admissions.entity.SpecialtyCluster;
import com.hospital.admissions.entity.WardClass;
import com.hospital.admissions.repository.AdmissionRequestRepository;
import com.hospital.admissions.repository.PatientRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies {@link PatientSpecifications#withoutActiveAdmission()} generates valid SQL against the
 * real schema and preserves the semantics of the JPQL {@code NOT IN} subquery it replaced: patients
 * with any admission request (including terminal ones) are excluded from the awaiting-assessment board.
 */
@SpringBootTest
@ActiveProfiles("prototype")
class PatientSpecificationsIntegrationTest {

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private AdmissionRequestRepository admissionRequestRepository;

    @Test
    @DisplayName("withoutActiveAdmission includes patients with no admission request and excludes those with one")
    void withoutActiveAdmissionFiltersByAdmissionRequestPresence() {
        Patient waiting = patientRepository.save(newPatient("Spec Waiting Patient"));
        Patient admitted = patientRepository.save(newPatient("Spec Admitted Patient"));

        admissionRequestRepository.save(AdmissionRequest.builder()
                .patient(admitted)
                .suspectedDiagnosisService(SpecialtyCluster.CARDIOLOGY)
                .primaryAcuityTier(AcuityTier.TIER_2_ACUTE_URGENT)
                .requestedWardClass(WardClass.B2)
                .status(AdmissionStatus.DISCHARGED)
                .requestedAt(LocalDateTime.now().minusHours(2))
                .build());

        List<Patient> result =
                patientRepository.findAll(PatientSpecifications.withoutActiveAdmission());

        assertThat(result).extracting(Patient::getId).contains(waiting.getId());
        assertThat(result).extracting(Patient::getId).doesNotContain(admitted.getId());
    }

    private Patient newPatient(String name) {
        return Patient.builder()
                .name(name)
                .nricMasked("S****999Z")
                .age(60)
                .gender(Gender.FEMALE)
                .infectionStatus(InfectionStatus.NON_INFECTIOUS)
                .fallRiskScore(20)
                .needsTelemetry(false)
                .queueToken("Q-SPEC-" + UUID.randomUUID())
                .build();
    }
}
