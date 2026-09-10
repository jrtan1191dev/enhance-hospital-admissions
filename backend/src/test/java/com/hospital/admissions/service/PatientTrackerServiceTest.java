package com.hospital.admissions.service;

import com.hospital.admissions.entity.*;
import com.hospital.admissions.dto.PatientMilestoneResponse;
import com.hospital.admissions.repository.AdmissionRequestRepository;
import com.hospital.admissions.repository.BedRepository;
import com.hospital.admissions.repository.PatientAuditInteractionRepository;
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
class PatientTrackerServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private AdmissionRequestRepository admissionRequestRepository;

    @Mock
    private BedRepository bedRepository;

    @Mock
    private PatientAuditInteractionRepository patientAuditInteractionRepository;

    @Mock
    private AuditLogger auditLogger;

    @InjectMocks
    private PatientTrackerService trackerService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("nurse_test", null, List.of())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("trackPatient throws when token is not found")
    void testTrackPatient_TokenNotFound() {
        when(patientRepository.findByQueueToken("TOKEN-NONE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trackerService.trackPatient("TOKEN-NONE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Patient not found for token");
    }

    @Test
    @DisplayName("trackPatient without AdmissionRequest returns ASSESSMENT_PENDING status")
    void testTrackPatient_NoAdmissionRequest() {
        UUID patientId = UUID.randomUUID();
        Patient patient = Patient.builder().id(patientId).name("Alice").queueToken("TOKEN-A").build();

        when(patientRepository.findByQueueToken("TOKEN-A")).thenReturn(Optional.of(patient));
        when(admissionRequestRepository.findByPatient_QueueToken("TOKEN-A")).thenReturn(Optional.empty());

        PatientMilestoneResponse res = trackerService.trackPatient("TOKEN-A");

        assertThat(res.getAdmissionStatus()).isEqualTo(AdmissionStatus.ASSESSMENT_PENDING);
        assertThat(res.getQueuePosition()).isEqualTo(1);
        assertThat(res.getEstimatedWaitMinutes()).isEqualTo(15);
        assertThat(res.getAssignedBedNumber()).isNull();
    }

    @Test
    @DisplayName("trackPatient with BED_REQUESTED calculates queue position and wait minutes")
    void testTrackPatient_BedRequested() {
        UUID patientId = UUID.randomUUID();
        Patient patient = Patient.builder().id(patientId).name("Alice").queueToken("TOKEN-A").build();

        UUID reqId = UUID.randomUUID();
        AdmissionRequest myReq = AdmissionRequest.builder()
                .id(reqId)
                .patient(patient)
                .status(AdmissionStatus.BED_REQUESTED)
                .primaryAcuityTier(AcuityTier.TIER_2_ACUTE_URGENT)
                .requestedWardClass(WardClass.B2)
                .requestedAt(LocalDateTime.now())
                .build();

        AdmissionRequest otherReq = AdmissionRequest.builder()
                .id(UUID.randomUUID())
                .primaryAcuityTier(AcuityTier.TIER_1_CRITICAL)
                .requestedWardClass(WardClass.B2)
                .requestedAt(LocalDateTime.now().minusMinutes(5))
                .build();

        when(patientRepository.findByQueueToken("TOKEN-A")).thenReturn(Optional.of(patient));
        when(admissionRequestRepository.findByPatient_QueueToken("TOKEN-A")).thenReturn(Optional.of(myReq));
        when(admissionRequestRepository.findByStatus(AdmissionStatus.BED_REQUESTED))
                .thenReturn(List.of(otherReq, myReq));

        PatientMilestoneResponse res = trackerService.trackPatient("TOKEN-A");

        assertThat(res.getAdmissionStatus()).isEqualTo(AdmissionStatus.BED_REQUESTED);
        assertThat(res.getQueuePosition()).isEqualTo(2);
        assertThat(res.getPatientsAhead()).isEqualTo(1);
        assertThat(res.getRequestedWardClass()).isEqualTo(WardClass.B2);
        assertThat(res.getEstimatedWaitMinutes()).isEqualTo(50); // position 2 * 25
    }

    @Test
    @DisplayName("trackPatient partitions queue by ward class and sorts by effective acuity tier")
    void testTrackPatient_WardClassPartitioningAndEffectiveAcuity() {
        UUID patientIdA = UUID.randomUUID();
        Patient patientA = Patient.builder().id(patientIdA).name("Patient A").queueToken("TOKEN-W1").build();

        // Target patient: Class B2, Primary Tier 3, Effective Tier 1 (escalated), requested at T-10m
        AdmissionRequest myReq = AdmissionRequest.builder()
                .id(UUID.randomUUID())
                .patient(patientA)
                .status(AdmissionStatus.BED_REQUESTED)
                .primaryAcuityTier(AcuityTier.TIER_3_ACUTE_STABLE)
                .effectiveAcuityTier(AcuityTier.TIER_1_CRITICAL)
                .requestedWardClass(WardClass.B2)
                .requestedAt(LocalDateTime.now().minusMinutes(10))
                .build();

        // Other patient 1: Class B2, Effective Tier 2 (lower urgency than Tier 1), requested earlier at T-20m
        AdmissionRequest b2OtherReq = AdmissionRequest.builder()
                .id(UUID.randomUUID())
                .patient(Patient.builder().id(UUID.randomUUID()).build())
                .status(AdmissionStatus.BED_REQUESTED)
                .primaryAcuityTier(AcuityTier.TIER_2_ACUTE_URGENT)
                .effectiveAcuityTier(AcuityTier.TIER_2_ACUTE_URGENT)
                .requestedWardClass(WardClass.B2)
                .requestedAt(LocalDateTime.now().minusMinutes(20))
                .build();

        // Other patient 2: Class A (different ward class), Tier 1, requested at T-30m -> MUST NOT affect B2 queue
        AdmissionRequest classAOtherReq = AdmissionRequest.builder()
                .id(UUID.randomUUID())
                .patient(Patient.builder().id(UUID.randomUUID()).build())
                .status(AdmissionStatus.BED_REQUESTED)
                .primaryAcuityTier(AcuityTier.TIER_1_CRITICAL)
                .effectiveAcuityTier(AcuityTier.TIER_1_CRITICAL)
                .requestedWardClass(WardClass.A)
                .requestedAt(LocalDateTime.now().minusMinutes(30))
                .build();

        when(patientRepository.findByQueueToken("TOKEN-W1")).thenReturn(Optional.of(patientA));
        when(admissionRequestRepository.findByPatient_QueueToken("TOKEN-W1")).thenReturn(Optional.of(myReq));
        when(admissionRequestRepository.findByStatus(AdmissionStatus.BED_REQUESTED))
                .thenReturn(List.of(classAOtherReq, b2OtherReq, myReq));

        PatientMilestoneResponse res = trackerService.trackPatient("TOKEN-W1");

        // Target patient should be #1 in Class B2 queue despite requestedAt being later than b2OtherReq because Tier 1 > Tier 2
        // And classAOtherReq is ignored because of different ward class
        assertThat(res.getQueuePosition()).isEqualTo(1);
        assertThat(res.getPatientsAhead()).isEqualTo(0);
        assertThat(res.getRequestedWardClass()).isEqualTo(WardClass.B2);
    }

    @Test
    @DisplayName("trackPatient translates delay tags to compassionate disclosures and applies additive wait buffers")
    void testTrackPatient_OperationalDelayTagAndAdditiveBuffer() {
        UUID patientId = UUID.randomUUID();
        Patient patient = Patient.builder().id(patientId).name("Delayed Patient").queueToken("TOKEN-DELAY").build();

        AdmissionRequest req = AdmissionRequest.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .status(AdmissionStatus.BED_REQUESTED)
                .primaryAcuityTier(AcuityTier.TIER_2_ACUTE_URGENT)
                .requestedWardClass(WardClass.B2)
                .requestedAt(LocalDateTime.now())
                .delayReasonTag("HOUSEKEEPING_DELAY")
                .build();

        when(patientRepository.findByQueueToken("TOKEN-DELAY")).thenReturn(Optional.of(patient));
        when(admissionRequestRepository.findByPatient_QueueToken("TOKEN-DELAY")).thenReturn(Optional.of(req));
        when(admissionRequestRepository.findByStatus(AdmissionStatus.BED_REQUESTED)).thenReturn(List.of(req));

        PatientMilestoneResponse res = trackerService.trackPatient("TOKEN-DELAY");

        // Base wait: position 1 * 25 = 25m. Additive buffer for HOUSEKEEPING_DELAY: +20m -> 45m
        assertThat(res.getEstimatedWaitMinutes()).isEqualTo(45);
        assertThat(res.getDelayReason()).isEqualTo("Your ward bed is currently undergoing final housekeeping sanitization and linen preparation.");
        assertThat(res.getDelayContactHotline()).isEqualTo("+65 6321 4311");
    }

    @Test
    @DisplayName("trackPatient handles isolation UV cleaning and trauma surge delay buffers")
    void testTrackPatient_IsolationAndSurgeDelayBuffers() {
        UUID patientId = UUID.randomUUID();
        Patient patient = Patient.builder().id(patientId).name("Isolation Patient").queueToken("TOKEN-ISO").build();

        AdmissionRequest reqIso = AdmissionRequest.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .status(AdmissionStatus.BED_REQUESTED)
                .primaryAcuityTier(AcuityTier.TIER_1_CRITICAL)
                .requestedWardClass(WardClass.B2)
                .requestedAt(LocalDateTime.now())
                .delayReasonTag("SPECIALIZED_ISOLATION_CLEANING")
                .build();

        when(patientRepository.findByQueueToken("TOKEN-ISO")).thenReturn(Optional.of(patient));
        when(admissionRequestRepository.findByPatient_QueueToken("TOKEN-ISO")).thenReturn(Optional.of(reqIso));
        when(admissionRequestRepository.findByStatus(AdmissionStatus.BED_REQUESTED)).thenReturn(List.of(reqIso));

        PatientMilestoneResponse res = trackerService.trackPatient("TOKEN-ISO");

        // Base wait: position 1 * 25 = 25m. Buffer for SPECIALIZED_ISOLATION_CLEANING: +30m -> 55m
        assertThat(res.getEstimatedWaitMinutes()).isEqualTo(55);
        assertThat(res.getDelayReason()).contains("30-minute UV disinfection cycle");
    }

    @Test
    @DisplayName("dispatchPeriodicUpdates identifies waiting patients exceeding dwell threshold and emits audit logs")
    void testDispatchPeriodicUpdates_ThresholdAndAuditLogging() {
        LocalDateTime now = LocalDateTime.now();

        Patient p1 = Patient.builder().id(UUID.randomUUID()).queueToken("TOKEN-PER-1").build();
        AdmissionRequest eligibleReq = AdmissionRequest.builder()
                .id(UUID.randomUUID())
                .patient(p1)
                .status(AdmissionStatus.BED_REQUESTED)
                .requestedAt(now.minusMinutes(12))
                .build();

        Patient p2 = Patient.builder().id(UUID.randomUUID()).queueToken("TOKEN-PER-2").build();
        AdmissionRequest ineligibleReq = AdmissionRequest.builder()
                .id(UUID.randomUUID())
                .patient(p2)
                .status(AdmissionStatus.BED_REQUESTED)
                .requestedAt(now.minusMinutes(2)) // Only 2 mins dwell, threshold is 5 mins
                .build();

        when(admissionRequestRepository.findByStatus(AdmissionStatus.BED_REQUESTED))
                .thenReturn(List.of(eligibleReq, ineligibleReq));

        int dispatchedCount = trackerService.dispatchPeriodicUpdates(now, 5);

        assertThat(dispatchedCount).isEqualTo(1);
        assertThat(eligibleReq.getLastPeriodicUpdateSentAt()).isEqualTo(now);
        assertThat(ineligibleReq.getLastPeriodicUpdateSentAt()).isNull();

        verify(admissionRequestRepository).save(eligibleReq);
        verify(auditLogger).logAction(
                eq("SYSTEM_SCHEDULER"),
                eq("DISPATCH_PERIODIC_UPDATE"),
                eq("AdmissionRequest:" + eligibleReq.getId()),
                contains("DeliveryStatus=SUCCESS")
        );
    }

    @Test
    @DisplayName("trackPatient returns tailored financial subsidy copy and step-down Community Hospital benchmarks")
    void testTrackPatient_FinancialAndStepDownCareExplainer_CommunityHospital() {
        UUID patientId = UUID.randomUUID();
        Patient patient = Patient.builder().id(patientId).name("CH Diversion Patient").queueToken("TOKEN-CH").build();

        AdmissionRequest chReq = AdmissionRequest.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .status(AdmissionStatus.BED_REQUESTED)
                .primaryAcuityTier(AcuityTier.TIER_4_SUBACUTE_DIVERSION)
                .requestedWardClass(WardClass.B2)
                .requestedAt(LocalDateTime.now())
                .diversionRecommended(true)
                .diversionPathway(DiversionPathway.COMMUNITY_HOSPITAL)
                .build();

        when(patientRepository.findByQueueToken("TOKEN-CH")).thenReturn(Optional.of(patient));
        when(admissionRequestRepository.findByPatient_QueueToken("TOKEN-CH")).thenReturn(Optional.of(chReq));
        when(admissionRequestRepository.findByStatus(AdmissionStatus.BED_REQUESTED)).thenReturn(List.of(chReq));

        PatientMilestoneResponse res = trackerService.trackPatient("TOKEN-CH");

        assertThat(res.getRequestedWardClass()).isEqualTo(WardClass.B2);
        assertThat(res.getCoPayEstimate()).contains("70%").contains("MediShield Life");
        assertThat(res.getDiversionRecommended()).isTrue();
        assertThat(res.getDiversionPathway()).isEqualTo(DiversionPathway.COMMUNITY_HOSPITAL);
        assertThat(res.getCareGuidance()).contains("14 to 21").contains("Community Hospital");
    }

    @Test
    @DisplayName("trackPatient returns MIC@Home virtual ward guidance when recommended")
    void testTrackPatient_FinancialAndStepDownCareExplainer_MicAtHome() {
        UUID patientId = UUID.randomUUID();
        Patient patient = Patient.builder().id(patientId).name("MIC Patient").queueToken("TOKEN-MIC").build();

        AdmissionRequest micReq = AdmissionRequest.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .status(AdmissionStatus.BED_REQUESTED)
                .primaryAcuityTier(AcuityTier.TIER_4_SUBACUTE_DIVERSION)
                .requestedWardClass(WardClass.C)
                .requestedAt(LocalDateTime.now())
                .diversionRecommended(true)
                .diversionPathway(DiversionPathway.HOSPITAL_AT_HOME_MIC)
                .build();

        when(patientRepository.findByQueueToken("TOKEN-MIC")).thenReturn(Optional.of(patient));
        when(admissionRequestRepository.findByPatient_QueueToken("TOKEN-MIC")).thenReturn(Optional.of(micReq));
        when(admissionRequestRepository.findByStatus(AdmissionStatus.BED_REQUESTED)).thenReturn(List.of(micReq));

        PatientMilestoneResponse res = trackerService.trackPatient("TOKEN-MIC");

        assertThat(res.getRequestedWardClass()).isEqualTo(WardClass.C);
        assertThat(res.getCoPayEstimate()).contains("80%").contains("MediShield Life");
        assertThat(res.getDiversionRecommended()).isTrue();
        assertThat(res.getDiversionPathway()).isEqualTo(DiversionPathway.HOSPITAL_AT_HOME_MIC);
        assertThat(res.getCareGuidance()).contains("MIC@Home").contains("visiting nurse");
    }

    @Test
    @DisplayName("trackPatient updates first/last access timestamps, increments access count, and emits audit log")
    void testTrackPatient_AccessAuditingAndCounters() {
        UUID patientId = UUID.randomUUID();
        Patient patient = Patient.builder().id(patientId).name("David").queueToken("TOKEN-D").build();

        AdmissionRequest req = AdmissionRequest.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .status(AdmissionStatus.BED_REQUESTED)
                .primaryAcuityTier(AcuityTier.TIER_2_ACUTE_URGENT)
                .requestedWardClass(WardClass.B2)
                .requestedAt(LocalDateTime.now().minusMinutes(30))
                .firstTrackerAccessedAt(null)
                .trackerAccessCount(0)
                .build();

        when(patientRepository.findByQueueToken("TOKEN-D")).thenReturn(Optional.of(patient));
        when(admissionRequestRepository.findByPatient_QueueToken("TOKEN-D")).thenReturn(Optional.of(req));
        when(admissionRequestRepository.findByStatus(AdmissionStatus.BED_REQUESTED)).thenReturn(List.of(req));

        // First access
        trackerService.trackPatient("TOKEN-D");

        assertThat(req.getFirstTrackerAccessedAt()).isNotNull();
        assertThat(req.getLastTrackerAccessedAt()).isNotNull();
        assertThat(req.getTrackerAccessCount()).isEqualTo(1);
        verify(admissionRequestRepository, times(1)).save(req);
        verify(auditLogger, times(1)).logAction(eq("PUBLIC_TOKEN"), eq("TRACK_PATIENT_ACCESS"), eq("PatientToken:TOKEN-D"), contains("PatientId="));

        // Subsequent access
        trackerService.trackPatient("TOKEN-D");
        assertThat(req.getTrackerAccessCount()).isEqualTo(2);
        verify(admissionRequestRepository, times(2)).save(req);
        // Verify no duplicate audit log
        verify(auditLogger, times(1)).logAction(eq("PUBLIC_TOKEN"), eq("TRACK_PATIENT_ACCESS"), eq("PatientToken:TOKEN-D"), anyString());
    }

    @Test
    @DisplayName("trackPatient with BED_ALLOCATED returns wait 5m and bed location details")
    void testTrackPatient_BedAllocated() {
        UUID patientId = UUID.randomUUID();
        Patient patient = Patient.builder().id(patientId).name("Bob").queueToken("TOKEN-B").build();

        Ward ward = Ward.builder().name("Ward 8A").level(8).build();
        Bed bed = Bed.builder().bedNumber("8A-02").ward(ward).build();

        AdmissionRequest req = AdmissionRequest.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .status(AdmissionStatus.BED_ALLOCATED)
                .requestedWardClass(WardClass.B2)
                .assignedBed(bed)
                .build();

        when(patientRepository.findByQueueToken("TOKEN-B")).thenReturn(Optional.of(patient));
        when(admissionRequestRepository.findByPatient_QueueToken("TOKEN-B")).thenReturn(Optional.of(req));

        PatientMilestoneResponse res = trackerService.trackPatient("TOKEN-B");

        assertThat(res.getAdmissionStatus()).isEqualTo(AdmissionStatus.BED_ALLOCATED);
        assertThat(res.getQueuePosition()).isEqualTo(0);
        assertThat(res.getEstimatedWaitMinutes()).isEqualTo(5);
        assertThat(res.getAssignedBedNumber()).isEqualTo("8A-02");
        assertThat(res.getAssignedWardName()).isEqualTo("Ward 8A");
        assertThat(res.getAssignedLevel()).isEqualTo(8);
    }

    @Test
    @DisplayName("trackPatient with ADMITTED_INPATIENT or DISCHARGED returns 0 wait minutes")
    void testTrackPatient_InpatientOrDischarged() {
        UUID patientId = UUID.randomUUID();
        Patient patient = Patient.builder().id(patientId).name("Charlie").queueToken("TOKEN-C").build();

        AdmissionRequest req = AdmissionRequest.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .status(AdmissionStatus.ADMITTED_INPATIENT)
                .requestedWardClass(WardClass.B2)
                .build();

        when(patientRepository.findByQueueToken("TOKEN-C")).thenReturn(Optional.of(patient));
        when(admissionRequestRepository.findByPatient_QueueToken("TOKEN-C")).thenReturn(Optional.of(req));

        PatientMilestoneResponse res = trackerService.trackPatient("TOKEN-C");

        assertThat(res.getAdmissionStatus()).isEqualTo(AdmissionStatus.ADMITTED_INPATIENT);
        assertThat(res.getQueuePosition()).isEqualTo(0);
        assertThat(res.getEstimatedWaitMinutes()).isEqualTo(0);
    }

    @Test
    @DisplayName("checkinPatient transitions bed to OCCUPIED_TAKEN and updates AdmissionRequest")
    void testCheckinPatient_Success() {
        UUID bedId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        Patient patient = Patient.builder().id(patientId).build();
        Bed bed = Bed.builder().id(bedId).bedNumber("8A-01").currentPatient(patient).build();
        AdmissionRequest req = AdmissionRequest.builder().id(UUID.randomUUID()).build();

        when(bedRepository.findById(bedId)).thenReturn(Optional.of(bed));
        when(bedRepository.save(bed)).thenReturn(bed);
        when(admissionRequestRepository.findByPatient_Id(patientId)).thenReturn(Optional.of(req));

        Bed result = trackerService.checkinPatient(bedId);

        assertThat(result.getStatus()).isEqualTo(BedStatus.OCCUPIED_TAKEN);
        assertThat(req.getStatus()).isEqualTo(AdmissionStatus.ADMITTED_INPATIENT);
        assertThat(req.getAdmittedAt()).isNotNull();
        verify(admissionRequestRepository).save(req);
        verify(auditLogger).logAction(eq("nurse_test"), eq("CHECKIN_PATIENT"), anyString(), anyString());
    }

    @Test
    @DisplayName("checkinPatient throws when bed is not found")
    void testCheckinPatient_NotFound() {
        UUID bedId = UUID.randomUUID();
        when(bedRepository.findById(bedId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trackerService.checkinPatient(bedId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Bed not found");
    }

    @Test
    @DisplayName("vacatePatient transitions bed to EMPTY_PENDING_CLEANING and discharges request")
    void testVacatePatient_Success() {
        UUID bedId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        Patient patient = Patient.builder().id(patientId).build();
        Bed bed = Bed.builder().id(bedId).bedNumber("8A-01").currentPatient(patient).build();
        AdmissionRequest req = AdmissionRequest.builder().id(UUID.randomUUID()).build();

        when(bedRepository.findById(bedId)).thenReturn(Optional.of(bed));
        when(bedRepository.save(bed)).thenReturn(bed);
        when(admissionRequestRepository.findByPatient_Id(patientId)).thenReturn(Optional.of(req));

        Bed result = trackerService.vacatePatient(bedId);

        assertThat(result.getStatus()).isEqualTo(BedStatus.EMPTY_PENDING_CLEANING);
        assertThat(req.getStatus()).isEqualTo(AdmissionStatus.DISCHARGED);
        assertThat(req.getDischargedAt()).isNotNull();
        verify(admissionRequestRepository).save(req);
        verify(auditLogger).logAction(eq("nurse_test"), eq("VACATE_PATIENT"), anyString(), anyString());
    }

    @Test
    @DisplayName("vacatePatient throws when bed is not found")
    void testVacatePatient_NotFound() {
        UUID bedId = UUID.randomUUID();
        when(bedRepository.findById(bedId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trackerService.vacatePatient(bedId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Bed not found");
    }

    @Test
    @DisplayName("cleanBed transitions bed to EMPTY_CLEANED and clears current patient")
    void testCleanBed_Success() {
        UUID bedId = UUID.randomUUID();
        Patient patient = Patient.builder().id(UUID.randomUUID()).build();
        Bed bed = Bed.builder()
                .id(bedId)
                .bedNumber("8A-01")
                .status(BedStatus.EMPTY_PENDING_CLEANING)
                .currentPatient(patient)
                .build();

        when(bedRepository.findById(bedId)).thenReturn(Optional.of(bed));
        when(bedRepository.save(bed)).thenReturn(bed);

        Bed result = trackerService.cleanBed(bedId);

        assertThat(result.getStatus()).isEqualTo(BedStatus.EMPTY_CLEANED);
        assertThat(result.getLastCleanedAt()).isNotNull();
        assertThat(result.getCurrentPatient()).isNull();
        verify(auditLogger).logAction(eq("nurse_test"), eq("CLEAN_BED"), anyString(), anyString());
    }

    @Test
    @DisplayName("cleanBed throws when bed is not found")
    void testCleanBed_NotFound() {
        UUID bedId = UUID.randomUUID();
        when(bedRepository.findById(bedId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trackerService.cleanBed(bedId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Bed not found");
    }

    @Test
    @DisplayName("cleanBed throws IllegalArgumentException when bed is not in EMPTY_PENDING_CLEANING status")
    void testCleanBed_InvalidStatusThrows() {
        UUID bedId = UUID.randomUUID();
        Bed bed = Bed.builder()
                .id(bedId)
                .bedNumber("8A-01")
                .status(BedStatus.OCCUPIED_TAKEN)
                .build();
        when(bedRepository.findById(bedId)).thenReturn(Optional.of(bed));

        assertThatThrownBy(() -> trackerService.cleanBed(bedId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("terminal sanitization requires EMPTY_PENDING_CLEANING");
    }

    @Test
    @DisplayName("recordPatientAction persists interaction and emits CONNECT_MSW_HOTLINE audit log")
    void testRecordPatientAction_MswCall() {
        UUID patientId = UUID.randomUUID();
        UUID admissionId = UUID.randomUUID();
        Patient patient = Patient.builder().id(patientId).name("Action Patient").queueToken("TOKEN-ACT-1").build();
        AdmissionRequest req = AdmissionRequest.builder().id(admissionId).patient(patient).build();

        when(patientRepository.findByQueueToken("TOKEN-ACT-1")).thenReturn(Optional.of(patient));
        when(admissionRequestRepository.findByPatient_QueueToken("TOKEN-ACT-1")).thenReturn(Optional.of(req));
        when(patientAuditInteractionRepository.save(any(PatientAuditInteraction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PatientAuditInteraction result = trackerService.recordPatientAction("TOKEN-ACT-1", "MSW_CALL");

        assertThat(result.getToken()).isEqualTo("TOKEN-ACT-1");
        assertThat(result.getActionType()).isEqualTo("MSW_CALL");
        assertThat(result.getAdmissionId()).isEqualTo(admissionId);
        assertThat(result.getCreatedAt()).isNotNull();

        verify(patientAuditInteractionRepository).save(any(PatientAuditInteraction.class));
        verify(auditLogger).logAction(
                eq("PUBLIC_TOKEN"),
                eq("CONNECT_MSW_HOTLINE"),
                eq("Patient:" + patientId),
                contains("Service=MEDICAL_SOCIAL_WORK")
        );
    }

    @Test
    @DisplayName("recordPatientAction persists interaction and emits CONNECT_FINANCIAL_COUNSELING audit log")
    void testRecordPatientAction_FinanceCall() {
        UUID patientId = UUID.randomUUID();
        Patient patient = Patient.builder().id(patientId).name("Action Patient").queueToken("TOKEN-ACT-2").build();

        when(patientRepository.findByQueueToken("TOKEN-ACT-2")).thenReturn(Optional.of(patient));
        when(admissionRequestRepository.findByPatient_QueueToken("TOKEN-ACT-2")).thenReturn(Optional.empty());
        when(patientAuditInteractionRepository.save(any(PatientAuditInteraction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PatientAuditInteraction result = trackerService.recordPatientAction("TOKEN-ACT-2", "FINANCE_CALL");

        assertThat(result.getToken()).isEqualTo("TOKEN-ACT-2");
        assertThat(result.getActionType()).isEqualTo("FINANCE_CALL");
        assertThat(result.getAdmissionId()).isNull();

        verify(auditLogger).logAction(
                eq("PUBLIC_TOKEN"),
                eq("CONNECT_FINANCIAL_COUNSELING"),
                eq("Patient:" + patientId),
                contains("Service=FINANCIAL_COUNSELING")
        );
    }
}
