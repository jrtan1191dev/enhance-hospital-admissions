package com.hospital.admissions.service;

import com.hospital.admissions.domain.*;
import com.hospital.admissions.dto.EdAssessmentSubmitRequest;
import com.hospital.admissions.dto.SpecialistConsultRequest;
import com.hospital.admissions.repository.AdmissionRequestRepository;
import com.hospital.admissions.repository.AssessmentBroadcastRepository;
import com.hospital.admissions.repository.PatientRepository;
import com.hospital.admissions.security.AuditLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClinicianServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private AdmissionRequestRepository admissionRequestRepository;

    @Mock
    private AssessmentBroadcastRepository broadcastRepository;

    @Mock
    private AuditLogger auditLogger;

    @InjectMocks
    private ClinicianService clinicianService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("dr_test", null, List.of())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("getEdWaitingPatients returns unassessed patients")
    void testGetEdWaitingPatients() {
        Patient patient = Patient.builder().id(UUID.randomUUID()).name("Test Patient").build();
        when(patientRepository.findPatientsWithoutActiveAdmission()).thenReturn(List.of(patient));

        List<Patient> result = clinicianService.getEdWaitingPatients();

        assertThat(result).containsExactly(patient);
        verify(patientRepository).findPatientsWithoutActiveAdmission();
    }

    @Test
    @DisplayName("getEdSubmittedAdmissions returns submitted admissions ordered by requestedAt desc")
    void testGetEdSubmittedAdmissions() {
        AdmissionRequest req = AdmissionRequest.builder().id(UUID.randomUUID()).build();
        when(admissionRequestRepository.findAllByOrderByRequestedAtDesc()).thenReturn(List.of(req));

        List<AdmissionRequest> result = clinicianService.getEdSubmittedAdmissions();

        assertThat(result).containsExactly(req);
        verify(admissionRequestRepository).findAllByOrderByRequestedAtDesc();
    }

    @Test
    @DisplayName("submitEdAssessment with Direct Admission (requiresSpecialistConsult=false) does not create broadcast")
    void testSubmitEdAssessment_DirectAdmission_NoBroadcastCreated() {
        UUID patientId = UUID.randomUUID();
        Patient patient = Patient.builder()
                .id(patientId)
                .name("Direct Patient")
                .build();

        EdAssessmentSubmitRequest req = EdAssessmentSubmitRequest.builder()
                .patientId(patientId)
                .suspectedDiagnosisService(SpecialtyCluster.GENERAL_MEDICINE)
                .primaryAcuityTier(AcuityTier.TIER_3_ACUTE_STABLE)
                .requestedWardClass(WardClass.B1)
                .primaryTelemetry(false)
                .requiresSpecialistConsult(false)
                .build();

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(patientRepository.save(any(Patient.class))).thenReturn(patient);
        when(admissionRequestRepository.save(any(AdmissionRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdmissionRequest result = clinicianService.submitEdAssessment(req);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(AdmissionStatus.BED_REQUESTED);
        assertThat(result.getEffectiveAcuityTier()).isEqualTo(AcuityTier.TIER_3_ACUTE_STABLE);
        assertThat(result.getEffectiveTelemetry()).isFalse();
        assertThat(result.getRequiresSpecialistConsult()).isFalse();
        verify(broadcastRepository, never()).save(any());
        verify(auditLogger).logAction(eq("dr_test"), eq("SUBMIT_ED_ASSESSMENT"), anyString(), anyString());
    }

    @Test
    @DisplayName("submitEdAssessment with structured overrides emits OVERRIDE_CLINICAL_BASELINE audit")
    void testSubmitEdAssessment_WithOverrides_EmitsOverrideAudit() {
        UUID patientId = UUID.randomUUID();
        Patient patient = Patient.builder()
                .id(patientId)
                .name("Override Patient")
                .build();

        com.hospital.admissions.dto.ClinicalBaselineOverride override = com.hospital.admissions.dto.ClinicalBaselineOverride.builder()
                .field("acuityTier")
                .originalValue("TIER_2_ACUTE_URGENT")
                .submittedValue("TIER_3_ACUTE_STABLE")
                .overrideReason("Clinical stability confirmed")
                .build();

        EdAssessmentSubmitRequest req = EdAssessmentSubmitRequest.builder()
                .patientId(patientId)
                .suspectedDiagnosisService(SpecialtyCluster.CARDIOLOGY)
                .primaryAcuityTier(AcuityTier.TIER_3_ACUTE_STABLE)
                .requestedWardClass(WardClass.B2)
                .primaryTelemetry(true)
                .requiresSpecialistConsult(false)
                .overrides(List.of(override))
                .build();

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(patientRepository.save(any(Patient.class))).thenReturn(patient);
        when(admissionRequestRepository.save(any(AdmissionRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdmissionRequest result = clinicianService.submitEdAssessment(req);

        assertThat(result).isNotNull();
        assertThat(result.getIsRecommendationAccepted()).isFalse();
        verify(auditLogger).logAction(eq("dr_test"), eq("SUBMIT_ED_ASSESSMENT"), anyString(), anyString());
        verify(auditLogger).logAction(eq("dr_test"), eq("OVERRIDE_CLINICAL_BASELINE"), anyString(), contains("Field=acuityTier"));
    }

    @Test
    @DisplayName("submitEdAssessment succeeds and creates admission request & broadcast")
    void testSubmitEdAssessment_Success() {
        UUID patientId = UUID.randomUUID();
        Patient patient = Patient.builder()
                .id(patientId)
                .name("John Doe")
                .needsTelemetry(false)
                .build();

        EdAssessmentSubmitRequest req = EdAssessmentSubmitRequest.builder()
                .patientId(patientId)
                .suspectedDiagnosisService(SpecialtyCluster.CARDIOLOGY)
                .primaryAcuityTier(AcuityTier.TIER_2_ACUTE_URGENT)
                .requestedWardClass(WardClass.B2)
                .needsTelemetry(true)
                .requiresSpecialistConsult(true)
                .build();

        UUID reqId = UUID.randomUUID();
        AdmissionRequest savedRequest = AdmissionRequest.builder()
                .id(reqId)
                .patient(patient)
                .suspectedDiagnosisService(SpecialtyCluster.CARDIOLOGY)
                .primaryAcuityTier(AcuityTier.TIER_2_ACUTE_URGENT)
                .requestedWardClass(WardClass.B2)
                .status(AdmissionStatus.BED_REQUESTED)
                .requestedAt(LocalDateTime.now())
                .build();

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(patientRepository.save(patient)).thenReturn(patient);
        when(admissionRequestRepository.save(any(AdmissionRequest.class))).thenReturn(savedRequest);

        AdmissionRequest result = clinicianService.submitEdAssessment(req);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(reqId);
        assertThat(patient.isNeedsTelemetry()).isTrue();
        verify(patientRepository).save(patient);
        verify(admissionRequestRepository).save(any(AdmissionRequest.class));
        verify(broadcastRepository).save(any(AssessmentBroadcast.class));
        verify(auditLogger).logAction(eq("dr_test"), eq("SUBMIT_ED_ASSESSMENT"), anyString(), anyString());
    }

    @Test
    @DisplayName("submitEdAssessment throws when patient is not found")
    void testSubmitEdAssessment_PatientNotFound() {
        UUID patientId = UUID.randomUUID();
        EdAssessmentSubmitRequest req = EdAssessmentSubmitRequest.builder()
                .patientId(patientId)
                .build();

        when(patientRepository.findById(patientId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clinicianService.submitEdAssessment(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Patient not found");

        verify(admissionRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("getBroadcasts with null cluster returns all")
    void testGetBroadcasts_NullCluster() {
        AssessmentBroadcast b = AssessmentBroadcast.builder().id(UUID.randomUUID()).build();
        when(broadcastRepository.findAll()).thenReturn(List.of(b));

        List<AssessmentBroadcast> result = clinicianService.getBroadcasts(null);

        assertThat(result).containsExactly(b);
        verify(broadcastRepository).findAll();
        verify(broadcastRepository, never()).findByTargetCluster(any());
    }

    @Test
    @DisplayName("getBroadcasts with cluster filters by target cluster")
    void testGetBroadcasts_WithCluster() {
        AssessmentBroadcast b = AssessmentBroadcast.builder().id(UUID.randomUUID()).targetCluster(SpecialtyCluster.CARDIOLOGY).build();
        when(broadcastRepository.findByTargetCluster(SpecialtyCluster.CARDIOLOGY)).thenReturn(List.of(b));

        List<AssessmentBroadcast> result = clinicianService.getBroadcasts(SpecialtyCluster.CARDIOLOGY);

        assertThat(result).containsExactly(b);
        verify(broadcastRepository).findByTargetCluster(SpecialtyCluster.CARDIOLOGY);
    }

    @Test
    @DisplayName("claimBroadcast succeeds and updates status to CLAIMED")
    void testClaimBroadcast_Success() {
        UUID broadcastId = UUID.randomUUID();
        AssessmentBroadcast b = AssessmentBroadcast.builder()
                .id(broadcastId)
                .status(BroadcastStatus.OPEN)
                .build();

        when(broadcastRepository.findById(broadcastId)).thenReturn(Optional.of(b));
        when(broadcastRepository.save(b)).thenReturn(b);

        AssessmentBroadcast result = clinicianService.claimBroadcast(broadcastId);

        assertThat(result.getStatus()).isEqualTo(BroadcastStatus.CLAIMED);
        assertThat(result.getClaimedBySpecialistId()).isEqualTo("dr_test");
        assertThat(result.getClaimedAt()).isNotNull();
        verify(broadcastRepository).save(b);
        verify(auditLogger).logAction(eq("dr_test"), eq("CLAIM_BROADCAST"), anyString(), anyString());
    }

    @Test
    @DisplayName("claimBroadcast throws when broadcast is not found")
    void testClaimBroadcast_NotFound() {
        UUID broadcastId = UUID.randomUUID();
        when(broadcastRepository.findById(broadcastId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clinicianService.claimBroadcast(broadcastId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Broadcast not found");
    }

    @Test
    @DisplayName("submitConsult succeeds and completes broadcast and updates admission request")
    void testSubmitConsult_Success() {
        UUID broadcastId = UUID.randomUUID();
        AdmissionRequest admissionRequest = AdmissionRequest.builder()
                .id(UUID.randomUUID())
                .primaryAcuityTier(AcuityTier.TIER_2_ACUTE_URGENT)
                .build();

        AssessmentBroadcast b = AssessmentBroadcast.builder()
                .id(broadcastId)
                .admissionRequest(admissionRequest)
                .status(BroadcastStatus.CLAIMED)
                .build();

        SpecialistConsultRequest req = SpecialistConsultRequest.builder()
                .consultNotes("Confirm Acute Coronary Syndrome")
                .secondaryAcuityTier(AcuityTier.TIER_2_ACUTE_URGENT)
                .diversionRecommended(false)
                .build();

        when(broadcastRepository.findById(broadcastId)).thenReturn(Optional.of(b));
        when(broadcastRepository.save(b)).thenReturn(b);

        AssessmentBroadcast result = clinicianService.submitConsult(broadcastId, req);

        assertThat(result.getStatus()).isEqualTo(BroadcastStatus.COMPLETED);
        assertThat(result.getConsultNotes()).isEqualTo("Confirm Acute Coronary Syndrome");
        assertThat(admissionRequest.getSecondaryAcuityTier()).isEqualTo(AcuityTier.TIER_2_ACUTE_URGENT);
        assertThat(admissionRequest.isDiversionRecommended()).isFalse();
        verify(admissionRequestRepository).save(admissionRequest);
        verify(broadcastRepository).save(b);
        verify(auditLogger).logAction(eq("dr_test"), eq("SUBMIT_SPECIALIST_CONSULT"), anyString(), anyString());
    }

    @Test
    @DisplayName("submitConsult throws when broadcast is not found")
    void testSubmitConsult_NotFound() {
        UUID broadcastId = UUID.randomUUID();
        SpecialistConsultRequest req = SpecialistConsultRequest.builder().build();
        when(broadcastRepository.findById(broadcastId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clinicianService.submitConsult(broadcastId, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Broadcast not found");
    }
}
