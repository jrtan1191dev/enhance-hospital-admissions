package com.hospital.admissions.security;

import com.hospital.admissions.domain.*;
import com.hospital.admissions.dto.*;
import com.hospital.admissions.gateway.SisterHospitalGateway;
import com.hospital.admissions.repository.*;
import com.hospital.admissions.service.*;
import com.hospital.admissions.solver.BedAllocationSolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KpiAuditLoggingTest {

    @Mock
    private PatientRepository patientRepository;
    @Mock
    private AdmissionRequestRepository admissionRequestRepository;
    @Mock
    private AssessmentBroadcastRepository broadcastRepository;
    @Mock
    private BedRepository bedRepository;
    @Mock
    private PatientAuditInteractionRepository patientAuditInteractionRepository;
    @Mock
    private WardRepository wardRepository;
    @Mock
    private BmuAlgorithmConfigRepository configRepository;
    @Mock
    private BedAllocationSolver solver;
    @Mock
    private SisterHospitalGateway sisterHospitalGateway;
    @Mock
    private AuditLogger auditLogger;

    private ClinicianService clinicianService;
    private BmuService bmuService;
    private PatientTrackerService trackerService;
    private WardService wardService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("dr_auditor", null, List.of())
        );

        clinicianService = new ClinicianService(patientRepository, admissionRequestRepository, broadcastRepository, auditLogger);
        bmuService = new BmuService(admissionRequestRepository, bedRepository, wardRepository, configRepository, solver, sisterHospitalGateway, auditLogger);
        trackerService = new PatientTrackerService(patientRepository, admissionRequestRepository, bedRepository, patientAuditInteractionRepository, auditLogger);
        wardService = new WardService(admissionRequestRepository, bedRepository, wardRepository, auditLogger);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("SUBMIT_ED_ASSESSMENT logs PrimaryAcuity, WardClass, Cluster, RecommendedAccepted, and ElapsedMins")
    void testSubmitEdAssessment_Logging() {
        UUID patientId = UUID.randomUUID();
        Patient patient = Patient.builder().id(patientId).name("Tan Ah Kow").build();
        patient.setCreatedAt(LocalDateTime.now().minusMinutes(15));

        EdAssessmentSubmitRequest req = EdAssessmentSubmitRequest.builder()
                .patientId(patientId)
                .suspectedDiagnosisService(SpecialtyCluster.CARDIOLOGY)
                .primaryAcuityTier(AcuityTier.TIER_2_ACUTE_URGENT)
                .requestedWardClass(WardClass.B2)
                .needsTelemetry(true)
                .recommendedAccepted(true)
                .elapsedMins(14.5)
                .build();

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(admissionRequestRepository.save(any())).thenAnswer(inv -> {
            AdmissionRequest r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        clinicianService.submitEdAssessment(req);

        ArgumentCaptor<String> detailsCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditLogger).logAction(eq("dr_auditor"), eq("SUBMIT_ED_ASSESSMENT"), anyString(), detailsCaptor.capture());

        String details = detailsCaptor.getValue();
        assertThat(details).contains("PrimaryAcuity=TIER_2_ACUTE_URGENT")
                .contains("WardClass=B2")
                .contains("Cluster=CARDIOLOGY")
                .contains("RecommendedAccepted=true")
                .contains("ElapsedMins=14.5");
    }

    @Test
    @DisplayName("CLAIM_BROADCAST logs TargetCluster, Specialist, and ElapsedClaimMins")
    void testClaimBroadcast_Logging() {
        UUID bId = UUID.randomUUID();
        AssessmentBroadcast b = AssessmentBroadcast.builder()
                .id(bId)
                .targetCluster(SpecialtyCluster.SURGERY)
                .status(BroadcastStatus.OPEN)
                .build();
        b.setCreatedAt(LocalDateTime.now().minusMinutes(8));

        when(broadcastRepository.findById(bId)).thenReturn(Optional.of(b));
        when(broadcastRepository.save(any())).thenReturn(b);

        clinicianService.claimBroadcast(bId);

        ArgumentCaptor<String> claimCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditLogger).logAction(eq("dr_auditor"), eq("CLAIM_BROADCAST"), eq("AssessmentBroadcast:" + bId), claimCaptor.capture());
        assertThat(claimCaptor.getValue()).contains("TargetCluster=SURGERY")
                .contains("Specialist=dr_auditor")
                .contains("ElapsedClaimMins=");
    }

    @Test
    @DisplayName("SUBMIT_SPECIALIST_CONSULT logs Concordant boolean and DiversionEndorsed")
    void testSpecialistConsult_Logging() {
        UUID bId = UUID.randomUUID();
        AdmissionRequest req = AdmissionRequest.builder()
                .id(UUID.randomUUID())
                .primaryAcuityTier(AcuityTier.TIER_2_ACUTE_URGENT)
                .build();
        AssessmentBroadcast b = AssessmentBroadcast.builder()
                .id(bId)
                .admissionRequest(req)
                .status(BroadcastStatus.CLAIMED)
                .build();

        when(broadcastRepository.findById(bId)).thenReturn(Optional.of(b));
        when(broadcastRepository.save(any())).thenReturn(b);

        SpecialistConsultRequest consultReq = SpecialistConsultRequest.builder()
                .secondaryAcuityTier(AcuityTier.TIER_3_ACUTE_STABLE)
                .diversionRecommended(true)
                .consultNotes("Patient can be safely downgraded")
                .build();

        clinicianService.submitConsult(bId, consultReq);

        ArgumentCaptor<String> detailsCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditLogger).logAction(eq("dr_auditor"), eq("SUBMIT_SPECIALIST_CONSULT"), eq("AssessmentBroadcast:" + bId), detailsCaptor.capture());
        assertThat(detailsCaptor.getValue()).contains("PrimaryAcuity=TIER_2_ACUTE_URGENT")
                .contains("SecondaryAcuity=TIER_3_ACUTE_STABLE")
                .contains("Concordant=false")
                .contains("DiversionEndorsed=true");
        assertThat(req.getIsDiscordant()).isTrue();
    }

    @Test
    @DisplayName("ALLOCATE_BED vs OVERRIDE_ALLOCATION logs rank, score, and override reason")
    void testBmuAllocationLogging() {
        UUID reqId = UUID.randomUUID();
        UUID bedId = UUID.randomUUID();
        Ward ward = Ward.builder().name("Ward 8B").build();
        Bed bed = Bed.builder().id(bedId).bedNumber("8B-04").ward(ward).status(BedStatus.EMPTY_CLEANED).build();
        AdmissionRequest admissionRequest = AdmissionRequest.builder().id(reqId).status(AdmissionStatus.BED_REQUESTED).build();

        when(admissionRequestRepository.findById(reqId)).thenReturn(Optional.of(admissionRequest));
        when(bedRepository.findById(bedId)).thenReturn(Optional.of(bed));
        when(admissionRequestRepository.save(any())).thenReturn(admissionRequest);

        // Standard 1-click allocation (default rank 1)
        bmuService.allocateBed(reqId, bedId);

        ArgumentCaptor<String> allocCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditLogger).logAction(eq("dr_auditor"), eq("ALLOCATE_BED"), eq("AdmissionRequest:" + reqId), allocCaptor.capture());
        assertThat(allocCaptor.getValue()).contains("AssignedBed=8B-04")
                .contains("Ward=Ward 8B")
                .contains("Rank=1")
                .contains("Score=85.0")
                .contains("Override=false");

        // Override allocation with rank > 1
        bed.setStatus(BedStatus.EMPTY_CLEANED);
        bmuService.allocateBed(reqId, bedId, 3, 62.0, "NON_TOP_RANK_SELECTION");

        ArgumentCaptor<String> overrideCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditLogger).logAction(eq("dr_auditor"), eq("OVERRIDE_ALLOCATION"), eq("AdmissionRequest:" + reqId), overrideCaptor.capture());
        assertThat(overrideCaptor.getValue()).contains("AssignedBed=8B-04")
                .contains("Ward=Ward 8B")
                .contains("SelectedRank=3")
                .contains("OverrideReason=NON_TOP_RANK_SELECTION")
                .contains("Override=true");
    }

    @Test
    @DisplayName("TRACK_PATIENT_ACCESS logs on first visit and avoids log spam on subsequent polling")
    void testPatientTrackerLogging_Deduplication() {
        UUID pId = UUID.randomUUID();
        Patient patient = Patient.builder().id(pId).name("Mdm Siti").queueToken("TOK-SITI").build();
        AdmissionRequest req = AdmissionRequest.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .status(AdmissionStatus.BED_REQUESTED)
                .primaryAcuityTier(AcuityTier.TIER_3_ACUTE_STABLE)
                .requestedWardClass(WardClass.B2)
                .requestedAt(LocalDateTime.now().minusMinutes(125))
                .firstTrackerAccessedAt(null)
                .build();

        when(patientRepository.findByQueueToken("TOK-SITI")).thenReturn(Optional.of(patient));
        when(admissionRequestRepository.findByPatient_QueueToken("TOK-SITI")).thenReturn(Optional.of(req));
        when(admissionRequestRepository.findByStatus(AdmissionStatus.BED_REQUESTED)).thenReturn(List.of(req));

        // First tracker access logs event
        trackerService.trackPatient("TOK-SITI");

        ArgumentCaptor<String> accessCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditLogger, times(1)).logAction(eq("PUBLIC_TOKEN"), eq("TRACK_PATIENT_ACCESS"), eq("PatientToken:TOK-SITI"), accessCaptor.capture());
        assertThat(accessCaptor.getValue()).contains("PatientId=" + pId)
                .contains("AdmissionStatus=BED_REQUESTED")
                .contains("MilestoneStep=QUEUE_WAITING")
                .contains("QueuePos=1")
                .contains("EstWaitMins=25");

        // Subsequent polling check does NOT emit duplicate audit log
        assertThat(req.getFirstTrackerAccessedAt()).isNotNull();
        trackerService.trackPatient("TOK-SITI");
        verify(auditLogger, times(1)).logAction(eq("PUBLIC_TOKEN"), eq("TRACK_PATIENT_ACCESS"), anyString(), anyString());
    }

    @Test
    @DisplayName("VACATE_PATIENT and CLEAN_BED log turnover timestamps and SLA metrics")
    void testDischargeAndTurnoverLogging() {
        UUID bedId = UUID.randomUUID();
        Bed bed = Bed.builder()
                .id(bedId)
                .bedNumber("8A-05")
                .status(BedStatus.OCCUPIED_TAKEN)
                .build();
        when(bedRepository.findById(bedId)).thenReturn(Optional.of(bed));
        when(bedRepository.save(any())).thenReturn(bed);

        trackerService.vacatePatient(bedId);

        ArgumentCaptor<String> vacateCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditLogger).logAction(eq("dr_auditor"), eq("VACATE_PATIENT"), eq("Bed:" + bedId), vacateCaptor.capture());
        assertThat(vacateCaptor.getValue()).contains("BedNumber=8A-05")
                .contains("VacateTimestamp=")
                .contains("VacateHour=")
                .contains("DischargedBeforeNoon=");

        // Now test cleaning bed
        bed.setCleaningStartedAt(LocalDateTime.now().minusMinutes(20));
        trackerService.cleanBed(bedId);

        ArgumentCaptor<String> cleanCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditLogger).logAction(eq("dr_auditor"), eq("CLEAN_BED"), eq("Bed:" + bedId), cleanCaptor.capture());
        assertThat(cleanCaptor.getValue()).contains("BedNumber=8A-05")
                .contains("HousekeeperId=dr_auditor")
                .contains("ElapsedCleaningMins=20")
                .contains("Within30mSla=true");
    }

    @Test
    @DisplayName("RECORD_EDD logs PatientId, EDD, Confidence, and RunwayStage (KPI 20)")
    void testRecordEdd_Logging() {
        UUID pId = UUID.randomUUID();
        Patient patient = Patient.builder().id(pId).name("Uncle Seng").build();
        AdmissionRequest req = AdmissionRequest.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .status(AdmissionStatus.ADMITTED_INPATIENT)
                .build();

        when(admissionRequestRepository.findByPatient_Id(pId)).thenReturn(Optional.of(req));
        when(admissionRequestRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        java.time.LocalDate targetEdd = java.time.LocalDate.now().plusDays(2);
        EddUpdateRequest updateReq = EddUpdateRequest.builder()
                .edd(targetEdd)
                .eddConfidence(EddConfidence.HIGH)
                .rationale("Clinical condition stabilized")
                .build();

        wardService.updateEdd(pId, updateReq);

        ArgumentCaptor<String> eddCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditLogger).logAction(eq("dr_auditor"), eq("RECORD_EDD"), eq("AdmissionRequest:" + req.getId()), eddCaptor.capture());
        assertThat(eddCaptor.getValue()).contains("PatientId=" + pId)
                .contains("EDD=" + targetEdd)
                .contains("Confidence=HIGH")
                .contains("RunwayStage=RUNWAY_D2");
    }
}
