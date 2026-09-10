package com.hospital.admissions.domain;

import com.hospital.admissions.repository.AdmissionRequestRepository;
import com.hospital.admissions.repository.PatientRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("prototype")
@Transactional
class JpaAuditingIntegrationTest {

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private AdmissionRequestRepository admissionRequestRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String username) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                username, "creds", AuthorityUtils.createAuthorityList("ROLE_USER")
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("AuditorAware automatically populates createdBy, createdAt, lastModifiedBy, lastModifiedAt on create")
    void testAuditingOnCreate() {
        authenticateAs("dr_tan_ed");

        Patient patient = Patient.builder()
                .name("Audit Test Patient")
                .nricMasked("S****999A")
                .age(45)
                .gender(Gender.FEMALE)
                .infectionStatus(InfectionStatus.NON_INFECTIOUS)
                .fallRiskScore(10)
                .needsTelemetry(false)
                .queueToken("TOKEN-AUDIT-001")
                .build();

        Patient saved = patientRepository.saveAndFlush(patient);

        assertThat(saved.getCreatedBy()).isEqualTo("dr_tan_ed");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getLastModifiedBy()).isEqualTo("dr_tan_ed");
        assertThat(saved.getLastModifiedAt()).isNotNull();
    }

    @Test
    @DisplayName("AuditorAware updates lastModifiedBy and lastModifiedAt while keeping createdBy and createdAt immutable")
    void testAuditingOnUpdate() {
        authenticateAs("dr_tan_ed");

        Patient patient = Patient.builder()
                .name("Update Test Patient")
                .nricMasked("S****888B")
                .age(50)
                .gender(Gender.MALE)
                .infectionStatus(InfectionStatus.NON_INFECTIOUS)
                .fallRiskScore(20)
                .needsTelemetry(true)
                .queueToken("TOKEN-AUDIT-002")
                .build();

        Patient saved = patientRepository.saveAndFlush(patient);
        String originalCreatedBy = saved.getCreatedBy();
        LocalDateTime originalCreatedAt = saved.getCreatedAt();

        // Switch authenticated user
        authenticateAs("bmu_coord_wong");

        saved.setFallRiskScore(55);
        Patient updated = patientRepository.saveAndFlush(saved);

        assertThat(updated.getCreatedBy()).isEqualTo(originalCreatedBy);
        assertThat(updated.getCreatedAt()).isEqualTo(originalCreatedAt);
        assertThat(updated.getLastModifiedBy()).isEqualTo("bmu_coord_wong");
        assertThat(updated.getLastModifiedAt()).isNotNull();
    }

    @Test
    @DisplayName("AuditorAware captures creator for AdmissionRequest data model")
    void testAuditingAdmissionRequest() {
        authenticateAs("dr_tan_ed");

        Patient patient = patientRepository.saveAndFlush(Patient.builder()
                .name("Admission Audit Patient")
                .nricMasked("S****777C")
                .age(60)
                .gender(Gender.MALE)
                .infectionStatus(InfectionStatus.NON_INFECTIOUS)
                .fallRiskScore(15)
                .needsTelemetry(false)
                .queueToken("TOKEN-AUDIT-003")
                .build());

        AdmissionRequest request = AdmissionRequest.builder()
                .patient(patient)
                .suspectedDiagnosisService(SpecialtyCluster.CARDIOLOGY)
                .primaryAcuityTier(AcuityTier.TIER_2_ACUTE_URGENT)
                .requestedWardClass(WardClass.B2)
                .status(AdmissionStatus.BED_REQUESTED)
                .requestedAt(LocalDateTime.now())
                .build();

        AdmissionRequest savedRequest = admissionRequestRepository.saveAndFlush(request);

        assertThat(savedRequest.getCreatedBy()).isEqualTo("dr_tan_ed");
        assertThat(savedRequest.getCreatedAt()).isNotNull();
        assertThat(savedRequest.getLastModifiedBy()).isEqualTo("dr_tan_ed");
        assertThat(savedRequest.getLastModifiedAt()).isNotNull();
    }
}
