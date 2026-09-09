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
    @DisplayName("submitEdAssessment with consult-gated multi-cluster publishes OPEN broadcast to each cluster")
    void testSubmitEdAssessment_ConsultGated_MultiCluster_PublishesToEachCluster() {
        UUID patientId = UUID.randomUUID();
        Patient patient = Patient.builder()
                .id(patientId)
                .name("Multi-Cluster Patient")
                .build();

        EdAssessmentSubmitRequest req = EdAssessmentSubmitRequest.builder()
                .patientId(patientId)
                .suspectedDiagnosisService(SpecialtyCluster.CARDIOLOGY)
                .primaryAcuityTier(AcuityTier.TIER_2_ACUTE_URGENT)
                .requestedWardClass(WardClass.B1)
                .requiresSpecialistConsult(true)
                .targetClusters(java.util.Set.of(SpecialtyCluster.CARDIOLOGY, SpecialtyCluster.SURGERY))
                .build();

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(patientRepository.save(patient)).thenReturn(patient);
        when(admissionRequestRepository.save(any(AdmissionRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        AdmissionRequest result = clinicianService.submitEdAssessment(req);

        assertThat(result.getStatus()).isEqualTo(AdmissionStatus.ASSESSMENT_PENDING);
        org.mockito.ArgumentCaptor<AssessmentBroadcast> broadcastCaptor = org.mockito.ArgumentCaptor.forClass(AssessmentBroadcast.class);
        verify(broadcastRepository, times(2)).save(broadcastCaptor.capture());

        List<AssessmentBroadcast> savedBroadcasts = broadcastCaptor.getAllValues();
        assertThat(savedBroadcasts).extracting(AssessmentBroadcast::getTargetCluster)
                .containsExactlyInAnyOrder(SpecialtyCluster.CARDIOLOGY, SpecialtyCluster.SURGERY);
        assertThat(savedBroadcasts).allMatch(b -> b.getStatus() == BroadcastStatus.OPEN);
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
    @DisplayName("claimBroadcast throws IllegalStateException when broadcast is already claimed")
    void testClaimBroadcast_AlreadyClaimed_ThrowsConflict() {
        UUID broadcastId = UUID.randomUUID();
        AssessmentBroadcast b = AssessmentBroadcast.builder()
                .id(broadcastId)
                .status(BroadcastStatus.CLAIMED)
                .claimedBySpecialistId("dr_lim")
                .build();

        when(broadcastRepository.findById(broadcastId)).thenReturn(Optional.of(b));

        assertThatThrownBy(() -> clinicianService.claimBroadcast(broadcastId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already been claimed");
        verify(broadcastRepository, never()).save(any());
    }

    @Test
    @DisplayName("claimBroadcast throws ObjectOptimisticLockingFailureException on concurrent write conflict")
    void testClaimBroadcast_OptimisticLockingFailure_PropagatesException() {
        UUID broadcastId = UUID.randomUUID();
        AssessmentBroadcast b = AssessmentBroadcast.builder()
                .id(broadcastId)
                .status(BroadcastStatus.OPEN)
                .build();

        when(broadcastRepository.findById(broadcastId)).thenReturn(Optional.of(b));
        when(broadcastRepository.save(b)).thenThrow(new org.springframework.orm.ObjectOptimisticLockingFailureException(AssessmentBroadcast.class, broadcastId));

        assertThatThrownBy(() -> clinicianService.claimBroadcast(broadcastId))
                .isInstanceOf(org.springframework.orm.ObjectOptimisticLockingFailureException.class);
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

    @Test
    @DisplayName("submitConsult with structured diversion populates admission dossier and audit log")
    void testSubmitConsult_WithStructuredDiversion_PopulatesAdmissionDossierAndAudit() {
        UUID broadcastId = UUID.randomUUID();
        AdmissionRequest admissionRequest = AdmissionRequest.builder()
                .id(UUID.randomUUID())
                .primaryAcuityTier(AcuityTier.TIER_2_ACUTE_URGENT)
                .build();

        AssessmentBroadcast b = AssessmentBroadcast.builder()
                .id(broadcastId)
                .admissionRequest(admissionRequest)
                .status(BroadcastStatus.CLAIMED)
                .claimedBySpecialistId("dr_test")
                .build();

        SpecialistConsultRequest req = SpecialistConsultRequest.builder()
                .consultNotes("Patient stable for community step-down")
                .secondaryAcuityTier(AcuityTier.TIER_4_SUBACUTE_DIVERSION)
                .secondaryTelemetry(false)
                .diversionPathway(DiversionPathway.COMMUNITY_HOSPITAL)
                .diversionRecommended(true)
                .build();

        when(broadcastRepository.findById(broadcastId)).thenReturn(Optional.of(b));
        when(broadcastRepository.save(b)).thenReturn(b);

        AssessmentBroadcast result = clinicianService.submitConsult(broadcastId, req);

        assertThat(result.getStatus()).isEqualTo(BroadcastStatus.COMPLETED);
        assertThat(result.getSecondaryAcuityTier()).isEqualTo(AcuityTier.TIER_4_SUBACUTE_DIVERSION);
        assertThat(result.getSecondaryTelemetry()).isFalse();
        assertThat(result.getDiversionPathway()).isEqualTo(DiversionPathway.COMMUNITY_HOSPITAL);
        assertThat(admissionRequest.isDiversionRecommended()).isTrue();
        assertThat(admissionRequest.getDiversionPathway()).isEqualTo(DiversionPathway.COMMUNITY_HOSPITAL);
        assertThat(admissionRequest.getSecondaryAcuityTier()).isEqualTo(AcuityTier.TIER_4_SUBACUTE_DIVERSION);
        verify(admissionRequestRepository).save(admissionRequest);
        verify(broadcastRepository).save(b);
        verify(auditLogger).logAction(eq("dr_test"), eq("SUBMIT_SPECIALIST_CONSULT"), anyString(), contains("DiversionPathway=COMMUNITY_HOSPITAL"));
    }

    @Test
    @DisplayName("chainConsult creates linked OPEN broadcast and emits CHAIN_CONSULT audit log")
    void testChainConsult_Success() {
        UUID broadcastId = UUID.randomUUID();
        AdmissionRequest admissionRequest = AdmissionRequest.builder()
                .id(UUID.randomUUID())
                .status(AdmissionStatus.ASSESSMENT_PENDING)
                .build();

        AssessmentBroadcast parentBroadcast = AssessmentBroadcast.builder()
                .id(broadcastId)
                .admissionRequest(admissionRequest)
                .targetCluster(SpecialtyCluster.CARDIOLOGY)
                .status(BroadcastStatus.CLAIMED)
                .claimedBySpecialistId("dr_test")
                .build();

        com.hospital.admissions.dto.ChainConsultRequest req = com.hospital.admissions.dto.ChainConsultRequest.builder()
                .targetCluster(SpecialtyCluster.ORTHOPAEDICS)
                .rationale("Rule out hip fracture")
                .build();

        when(broadcastRepository.findById(broadcastId)).thenReturn(Optional.of(parentBroadcast));
        when(broadcastRepository.save(any(AssessmentBroadcast.class))).thenAnswer(inv -> inv.getArgument(0));

        AssessmentBroadcast chained = clinicianService.chainConsult(broadcastId, req);

        assertThat(chained).isNotNull();
        assertThat(chained.getStatus()).isEqualTo(BroadcastStatus.OPEN);
        assertThat(chained.getTargetCluster()).isEqualTo(SpecialtyCluster.ORTHOPAEDICS);
        assertThat(chained.getParentBroadcastId()).isEqualTo(broadcastId);
        assertThat(chained.getAdmissionRequest()).isEqualTo(admissionRequest);
        verify(broadcastRepository).save(any(AssessmentBroadcast.class));
        verify(auditLogger).logAction(eq("dr_test"), eq("CHAIN_CONSULT"), anyString(), contains("TargetCluster=ORTHOPAEDICS"));
    }

    @Test
    @DisplayName("chainConsult throws IllegalStateException when broadcast is not CLAIMED")
    void testChainConsult_UnclaimedBroadcast_ThrowsException() {
        UUID broadcastId = UUID.randomUUID();
        AssessmentBroadcast parentBroadcast = AssessmentBroadcast.builder()
                .id(broadcastId)
                .status(BroadcastStatus.OPEN)
                .build();

        com.hospital.admissions.dto.ChainConsultRequest req = com.hospital.admissions.dto.ChainConsultRequest.builder()
                .targetCluster(SpecialtyCluster.SURGERY)
                .rationale("Surgical consult needed")
                .build();

        when(broadcastRepository.findById(broadcastId)).thenReturn(Optional.of(parentBroadcast));

        assertThatThrownBy(() -> clinicianService.chainConsult(broadcastId, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be claimed");
    }

    @Test
    @DisplayName("autoEscalateOverdueBroadcasts escalates Tier 1-2 broadcasts exceeding 15 mins to designated default specialist")
    void testAutoEscalateOverdueBroadcasts_Tier1And2_EscalatesAfter15Minutes() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 9, 12, 30);
        LocalDateTime created16mAgo = now.minusMinutes(16);

        AdmissionRequest reqCardio = AdmissionRequest.builder()
                .id(UUID.randomUUID())
                .primaryAcuityTier(AcuityTier.TIER_2_ACUTE_URGENT)
                .build();

        AssessmentBroadcast cardioBroadcast = AssessmentBroadcast.builder()
                .id(UUID.randomUUID())
                .admissionRequest(reqCardio)
                .targetCluster(SpecialtyCluster.CARDIOLOGY)
                .status(BroadcastStatus.OPEN)
                .build();
        cardioBroadcast.setCreatedAt(created16mAgo);

        when(broadcastRepository.findByStatus(BroadcastStatus.OPEN)).thenReturn(List.of(cardioBroadcast));
        when(broadcastRepository.save(any(AssessmentBroadcast.class))).thenAnswer(inv -> inv.getArgument(0));

        int escalatedCount = clinicianService.autoEscalateOverdueBroadcasts(now);

        assertThat(escalatedCount).isEqualTo(1);
        assertThat(cardioBroadcast.getStatus()).isEqualTo(BroadcastStatus.AUTO_ESCALATED);
        assertThat(cardioBroadcast.getClaimedBySpecialistId()).isEqualTo("dr_lim_cardio");
        assertThat(cardioBroadcast.getClaimedAt()).isEqualTo(now);
        verify(broadcastRepository).save(cardioBroadcast);
        verify(auditLogger).logAction(eq("SYSTEM"), eq("AUTO_ESCALATE_BROADCAST"), eq("AssessmentBroadcast:" + cardioBroadcast.getId()), contains("EscalatedTo=dr_lim_cardio"));
    }

    @Test
    @DisplayName("autoEscalateOverdueBroadcasts respects 30 min SLA for Tiers 3-5 and verifies cluster mappings")
    void testAutoEscalateOverdueBroadcasts_Tiers3To5_SlaThresholdsAndClusterMappings() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 9, 12, 30);

        // Broadcast 1: General Medicine, Tier 3, created 25 mins ago (within 30m SLA) -> should NOT escalate
        AdmissionRequest reqGenMed = AdmissionRequest.builder()
                .id(UUID.randomUUID())
                .primaryAcuityTier(AcuityTier.TIER_3_ACUTE_STABLE)
                .build();
        AssessmentBroadcast genMedBroadcast = AssessmentBroadcast.builder()
                .id(UUID.randomUUID())
                .admissionRequest(reqGenMed)
                .targetCluster(SpecialtyCluster.GENERAL_MEDICINE)
                .status(BroadcastStatus.OPEN)
                .build();
        genMedBroadcast.setCreatedAt(now.minusMinutes(25));

        // Broadcast 2: Orthopaedics, Tier 4, created 35 mins ago (exceeds 30m SLA) -> should escalate to dr_lee_ortho
        AdmissionRequest reqOrtho = AdmissionRequest.builder()
                .id(UUID.randomUUID())
                .primaryAcuityTier(AcuityTier.TIER_4_SUBACUTE_DIVERSION)
                .build();
        AssessmentBroadcast orthoBroadcast = AssessmentBroadcast.builder()
                .id(UUID.randomUUID())
                .admissionRequest(reqOrtho)
                .targetCluster(SpecialtyCluster.ORTHOPAEDICS)
                .status(BroadcastStatus.OPEN)
                .build();
        orthoBroadcast.setCreatedAt(now.minusMinutes(35));

        // Broadcast 3: Surgery, Tier 1, created 20 mins ago (exceeds 15m SLA) -> should escalate to dr_kumar_surg
        AdmissionRequest reqSurg = AdmissionRequest.builder()
                .id(UUID.randomUUID())
                .primaryAcuityTier(AcuityTier.TIER_1_CRITICAL)
                .build();
        AssessmentBroadcast surgBroadcast = AssessmentBroadcast.builder()
                .id(UUID.randomUUID())
                .admissionRequest(reqSurg)
                .targetCluster(SpecialtyCluster.SURGERY)
                .status(BroadcastStatus.OPEN)
                .build();
        surgBroadcast.setCreatedAt(now.minusMinutes(20));

        when(broadcastRepository.findByStatus(BroadcastStatus.OPEN))
                .thenReturn(List.of(genMedBroadcast, orthoBroadcast, surgBroadcast));
        when(broadcastRepository.save(any(AssessmentBroadcast.class))).thenAnswer(inv -> inv.getArgument(0));

        int escalatedCount = clinicianService.autoEscalateOverdueBroadcasts(now);

        assertThat(escalatedCount).isEqualTo(2);
        // GenMed should remain OPEN
        assertThat(genMedBroadcast.getStatus()).isEqualTo(BroadcastStatus.OPEN);
        assertThat(genMedBroadcast.getClaimedBySpecialistId()).isNull();

        // Ortho escalated to dr_lee_ortho
        assertThat(orthoBroadcast.getStatus()).isEqualTo(BroadcastStatus.AUTO_ESCALATED);
        assertThat(orthoBroadcast.getClaimedBySpecialistId()).isEqualTo("dr_lee_ortho");

        // Surg escalated to dr_kumar_surg
        assertThat(surgBroadcast.getStatus()).isEqualTo(BroadcastStatus.AUTO_ESCALATED);
        assertThat(surgBroadcast.getClaimedBySpecialistId()).isEqualTo("dr_kumar_surg");
    }

    @Test
    @DisplayName("chainConsult allows chaining from AUTO_ESCALATED broadcast")
    void testChainConsult_AutoEscalatedBroadcast_Success() {
        UUID broadcastId = UUID.randomUUID();
        AdmissionRequest admissionRequest = AdmissionRequest.builder().id(UUID.randomUUID()).build();
        AssessmentBroadcast autoEscalatedBroadcast = AssessmentBroadcast.builder()
                .id(broadcastId)
                .admissionRequest(admissionRequest)
                .targetCluster(SpecialtyCluster.CARDIOLOGY)
                .status(BroadcastStatus.AUTO_ESCALATED)
                .claimedBySpecialistId("dr_lim_cardio")
                .build();

        com.hospital.admissions.dto.ChainConsultRequest req = com.hospital.admissions.dto.ChainConsultRequest.builder()
                .targetCluster(SpecialtyCluster.GENERAL_MEDICINE)
                .rationale("Auto-escalated multidisciplinary review required")
                .build();

        when(broadcastRepository.findById(broadcastId)).thenReturn(Optional.of(autoEscalatedBroadcast));
        when(broadcastRepository.save(any(AssessmentBroadcast.class))).thenAnswer(inv -> inv.getArgument(0));

        AssessmentBroadcast chained = clinicianService.chainConsult(broadcastId, req);

        assertThat(chained).isNotNull();
        assertThat(chained.getStatus()).isEqualTo(BroadcastStatus.OPEN);
        assertThat(chained.getTargetCluster()).isEqualTo(SpecialtyCluster.GENERAL_MEDICINE);
        assertThat(chained.getParentBroadcastId()).isEqualTo(broadcastId);
        verify(broadcastRepository).save(any(AssessmentBroadcast.class));
    }

    @Test
    @DisplayName("Consensus Completion Gate: Partial broadcast completion leaves AdmissionRequest in ASSESSMENT_PENDING")
    void testSubmitConsult_ConsensusGate_PartialCompletion_RemainsAssessmentPending() {
        UUID admissionId = UUID.randomUUID();
        AdmissionRequest admissionRequest = AdmissionRequest.builder()
                .id(admissionId)
                .status(AdmissionStatus.ASSESSMENT_PENDING)
                .primaryAcuityTier(AcuityTier.TIER_3_ACUTE_STABLE)
                .primaryTelemetry(false)
                .build();

        UUID b1Id = UUID.randomUUID();
        AssessmentBroadcast b1 = AssessmentBroadcast.builder()
                .id(b1Id)
                .admissionRequest(admissionRequest)
                .targetCluster(SpecialtyCluster.CARDIOLOGY)
                .status(BroadcastStatus.CLAIMED)
                .claimedBySpecialistId("dr_test")
                .build();

        UUID b2Id = UUID.randomUUID();
        AssessmentBroadcast b2 = AssessmentBroadcast.builder()
                .id(b2Id)
                .admissionRequest(admissionRequest)
                .targetCluster(SpecialtyCluster.ORTHOPAEDICS)
                .status(BroadcastStatus.OPEN)
                .build();

        SpecialistConsultRequest req = SpecialistConsultRequest.builder()
                .secondaryAcuityTier(AcuityTier.TIER_2_ACUTE_URGENT)
                .secondaryTelemetry(true)
                .consultNotes("Cardiac stable, awaiting orthopaedic consult")
                .build();

        when(broadcastRepository.findById(b1Id)).thenReturn(Optional.of(b1));
        when(broadcastRepository.save(b1)).thenReturn(b1);
        when(broadcastRepository.findByAdmissionRequest_Id(admissionId)).thenReturn(List.of(b1, b2));

        AssessmentBroadcast result = clinicianService.submitConsult(b1Id, req);

        assertThat(result.getStatus()).isEqualTo(BroadcastStatus.COMPLETED);
        // Gate remains closed because b2 is still OPEN
        assertThat(admissionRequest.getStatus()).isEqualTo(AdmissionStatus.ASSESSMENT_PENDING);
        // Effective acuity and telemetry are updated
        assertThat(admissionRequest.getEffectiveAcuityTier()).isEqualTo(AcuityTier.TIER_2_ACUTE_URGENT);
        assertThat(admissionRequest.getEffectiveTelemetry()).isTrue();
        assertThat(admissionRequest.getIsDiscordant()).isTrue();
        verify(admissionRequestRepository).save(admissionRequest);
    }

    @Test
    @DisplayName("Consensus Completion Gate: All broadcasts completed advances AdmissionRequest to BED_REQUESTED with highest acuity and telemetry union")
    void testSubmitConsult_ConsensusGate_AllCompleted_AdvancesToBedRequested_AndElevatesAcuityAndTelemetry() {
        UUID admissionId = UUID.randomUUID();
        AdmissionRequest admissionRequest = AdmissionRequest.builder()
                .id(admissionId)
                .status(AdmissionStatus.ASSESSMENT_PENDING)
                .primaryAcuityTier(AcuityTier.TIER_3_ACUTE_STABLE)
                .primaryTelemetry(false)
                .build();

        UUID b1Id = UUID.randomUUID();
        AssessmentBroadcast b1 = AssessmentBroadcast.builder()
                .id(b1Id)
                .admissionRequest(admissionRequest)
                .targetCluster(SpecialtyCluster.CARDIOLOGY)
                .status(BroadcastStatus.COMPLETED)
                .secondaryAcuityTier(AcuityTier.TIER_3_ACUTE_STABLE)
                .secondaryTelemetry(false)
                .claimedBySpecialistId("dr_lim_cardio")
                .build();

        UUID b2Id = UUID.randomUUID();
        AssessmentBroadcast b2 = AssessmentBroadcast.builder()
                .id(b2Id)
                .admissionRequest(admissionRequest)
                .targetCluster(SpecialtyCluster.SURGERY)
                .status(BroadcastStatus.CLAIMED)
                .claimedBySpecialistId("dr_kumar_surg")
                .build();

        // Surgery specialist elevates acuity to TIER_1_CRITICAL with telemetry required
        SpecialistConsultRequest req = SpecialistConsultRequest.builder()
                .secondaryAcuityTier(AcuityTier.TIER_1_CRITICAL)
                .secondaryTelemetry(true)
                .consultNotes("Acute abdomen with signs of peritonitis: Resuscitation required!")
                .build();

        when(broadcastRepository.findById(b2Id)).thenReturn(Optional.of(b2));
        when(broadcastRepository.save(b2)).thenReturn(b2);
        when(broadcastRepository.findByAdmissionRequest_Id(admissionId)).thenReturn(List.of(b1, b2));

        AssessmentBroadcast result = clinicianService.submitConsult(b2Id, req);

        assertThat(result.getStatus()).isEqualTo(BroadcastStatus.COMPLETED);
        // Gate opens: All broadcasts are now COMPLETED!
        assertThat(admissionRequest.getStatus()).isEqualTo(AdmissionStatus.BED_REQUESTED);
        // Safety-First: Effective acuity elevated to highest acuity (TIER_1_CRITICAL)
        assertThat(admissionRequest.getEffectiveAcuityTier()).isEqualTo(AcuityTier.TIER_1_CRITICAL);
        // Safety-First: Telemetry unioned to true
        assertThat(admissionRequest.getEffectiveTelemetry()).isTrue();
        // Discordance flagged due to divergence from primary ED tier
        assertThat(admissionRequest.getIsDiscordant()).isTrue();
        verify(admissionRequestRepository).save(admissionRequest);
    }

    @Test
    @DisplayName("amendConsult: In-place consult amendment updates effective acuity, preserves dwell time requestedAt, sets conditionUpdated, and logs audit")
    void testAmendConsult_UpdatesEffectiveAcuity_PreservesRequestedAt_SetsConditionUpdated() {
        UUID admissionId = UUID.randomUUID();
        LocalDateTime originalRequestedAt = LocalDateTime.of(2026, 9, 9, 14, 0);

        AdmissionRequest admissionRequest = AdmissionRequest.builder()
                .id(admissionId)
                .primaryAcuityTier(AcuityTier.TIER_3_ACUTE_STABLE)
                .primaryTelemetry(false)
                .effectiveAcuityTier(AcuityTier.TIER_2_ACUTE_URGENT)
                .effectiveTelemetry(false)
                .isDiscordant(true)
                .status(AdmissionStatus.BED_REQUESTED)
                .requestedAt(originalRequestedAt)
                .clinicalConditionUpdated(false)
                .build();

        UUID broadcastId = UUID.randomUUID();
        AssessmentBroadcast broadcast = AssessmentBroadcast.builder()
                .id(broadcastId)
                .admissionRequest(admissionRequest)
                .targetCluster(SpecialtyCluster.CARDIOLOGY)
                .status(BroadcastStatus.COMPLETED)
                .secondaryAcuityTier(AcuityTier.TIER_2_ACUTE_URGENT)
                .secondaryTelemetry(false)
                .consultNotes("Initial assessment: Acute chest pain.")
                .claimedBySpecialistId("dr_lim_cardio")
                .build();

        // Specialist amends: Troponin now positive, patient elevated to Tier 1 Critical with Telemetry
        SpecialistConsultRequest amendReq = SpecialistConsultRequest.builder()
                .secondaryAcuityTier(AcuityTier.TIER_1_CRITICAL)
                .secondaryTelemetry(true)
                .consultNotes("Patient troponin peaked, acute STEMI detected. Elevate to ICU/telemetry bed immediately.")
                .build();

        when(broadcastRepository.findById(broadcastId)).thenReturn(Optional.of(broadcast));
        when(broadcastRepository.save(broadcast)).thenReturn(broadcast);
        when(broadcastRepository.findByAdmissionRequest_Id(admissionId)).thenReturn(List.of(broadcast));

        AssessmentBroadcast amended = clinicianService.amendConsult(broadcastId, amendReq);

        assertThat(amended.getSecondaryAcuityTier()).isEqualTo(AcuityTier.TIER_1_CRITICAL);
        assertThat(amended.getSecondaryTelemetry()).isTrue();
        assertThat(amended.getConsultNotes()).contains("STEMI detected");

        // AdmissionRequest verification
        assertThat(admissionRequest.getEffectiveAcuityTier()).isEqualTo(AcuityTier.TIER_1_CRITICAL);
        assertThat(admissionRequest.getEffectiveTelemetry()).isTrue();
        assertThat(admissionRequest.getIsDiscordant()).isTrue();
        assertThat(admissionRequest.getClinicalConditionUpdated()).isTrue();
        // Preserves queue dwell time
        assertThat(admissionRequest.getRequestedAt()).isEqualTo(originalRequestedAt);

        verify(admissionRequestRepository).save(admissionRequest);
        verify(auditLogger).logAction(eq("dr_test"), eq("AMEND_SPECIALIST_CONSULT"), eq("AssessmentBroadcast:" + broadcastId), anyString());
    }

    @Test
    @DisplayName("amendConsult: When amendment aligns secondary assessment with primary ED assessment, discordance automatically clears")
    void testAmendConsult_WhenAlignsWithPrimary_ClearsDiscordance() {
        UUID admissionId = UUID.randomUUID();
        LocalDateTime originalRequestedAt = LocalDateTime.of(2026, 9, 9, 14, 0);

        AdmissionRequest admissionRequest = AdmissionRequest.builder()
                .id(admissionId)
                .primaryAcuityTier(AcuityTier.TIER_3_ACUTE_STABLE)
                .primaryTelemetry(false)
                .effectiveAcuityTier(AcuityTier.TIER_2_ACUTE_URGENT)
                .effectiveTelemetry(true)
                .isDiscordant(true)
                .status(AdmissionStatus.BED_REQUESTED)
                .requestedAt(originalRequestedAt)
                .build();

        UUID broadcastId = UUID.randomUUID();
        AssessmentBroadcast broadcast = AssessmentBroadcast.builder()
                .id(broadcastId)
                .admissionRequest(admissionRequest)
                .targetCluster(SpecialtyCluster.CARDIOLOGY)
                .status(BroadcastStatus.COMPLETED)
                .secondaryAcuityTier(AcuityTier.TIER_2_ACUTE_URGENT)
                .secondaryTelemetry(true)
                .consultNotes("Initial discordance.")
                .claimedBySpecialistId("dr_lim_cardio")
                .build();

        // Specialist amends to align with ED attending assessment: Tier 3 and false telemetry
        SpecialistConsultRequest amendReq = SpecialistConsultRequest.builder()
                .secondaryAcuityTier(AcuityTier.TIER_3_ACUTE_STABLE)
                .secondaryTelemetry(false)
                .consultNotes("Repeat ECG normal, concordant with ED attending's initial assessment.")
                .build();

        when(broadcastRepository.findById(broadcastId)).thenReturn(Optional.of(broadcast));
        when(broadcastRepository.save(broadcast)).thenReturn(broadcast);
        when(broadcastRepository.findByAdmissionRequest_Id(admissionId)).thenReturn(List.of(broadcast));

        clinicianService.amendConsult(broadcastId, amendReq);

        assertThat(admissionRequest.getEffectiveAcuityTier()).isEqualTo(AcuityTier.TIER_3_ACUTE_STABLE);
        assertThat(admissionRequest.getEffectiveTelemetry()).isFalse();
        assertThat(admissionRequest.getIsDiscordant()).isFalse(); // Auto-cleared discordance!
        assertThat(admissionRequest.getClinicalConditionUpdated()).isTrue();
    }
}

