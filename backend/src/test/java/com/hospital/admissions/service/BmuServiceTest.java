package com.hospital.admissions.service;

import com.hospital.admissions.entity.*;
import com.hospital.admissions.dto.BedRecommendation;
import com.hospital.admissions.dto.BmuConfigUpdateRequest;
import com.hospital.admissions.dto.SisterHospitalReferralResponse;
import com.hospital.admissions.dto.WardDto;
import com.hospital.admissions.gateway.SisterHospitalGateway;
import com.hospital.admissions.repository.AdmissionRequestRepository;
import com.hospital.admissions.repository.BedRepository;
import com.hospital.admissions.repository.BmuAlgorithmConfigRepository;
import com.hospital.admissions.repository.WardRepository;
import com.hospital.admissions.security.AuditLogger;
import com.hospital.admissions.solver.BedAllocationSolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import com.hospital.admissions.dto.DelayTagRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BmuServiceTest {

    @Mock
    private AdmissionRequestRepository admissionRequestRepository;

    @Mock
    private BedRepository bedRepository;

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

    @InjectMocks
    private BmuService bmuService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("bmu_coord", null, List.of(new SimpleGrantedAuthority("ROLE_BMU_COORDINATOR")))
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("getPrioritizedQueue returns requests sorted by AcuityTier ordinal then requestedAt")
    void testGetPrioritizedQueue() {
        LocalDateTime now = LocalDateTime.now();
        AdmissionRequest r1 = AdmissionRequest.builder()
                .id(UUID.randomUUID())
                .primaryAcuityTier(AcuityTier.TIER_3_ACUTE_STABLE)
                .requestedAt(now.minusMinutes(10))
                .build();
        AdmissionRequest r2 = AdmissionRequest.builder()
                .id(UUID.randomUUID())
                .primaryAcuityTier(AcuityTier.TIER_1_CRITICAL)
                .requestedAt(now.minusMinutes(5))
                .build();
        AdmissionRequest r3 = AdmissionRequest.builder()
                .id(UUID.randomUUID())
                .primaryAcuityTier(AcuityTier.TIER_1_CRITICAL)
                .requestedAt(now.minusMinutes(20))
                .build();

        when(admissionRequestRepository.findByStatus(AdmissionStatus.BED_REQUESTED))
                .thenReturn(List.of(r1, r2, r3));

        List<AdmissionRequest> queue = bmuService.getPrioritizedQueue();

        // Expect: r3 (Tier 1, -20m) -> r2 (Tier 1, -5m) -> r1 (Tier 3, -10m)
        assertThat(queue).containsExactly(r3, r2, r1);
    }

    @Test
    @DisplayName("getPrioritizedQueue sorts by effectiveAcuityTier over primaryAcuityTier under Safety-First policy")
    void testGetPrioritizedQueue_SortsByEffectiveAcuityTierOverPrimaryAcuityTier() {
        LocalDateTime now = LocalDateTime.now();

        // Request A: primary Tier 3, but escalated to Tier 1 via specialist consult
        AdmissionRequest reqA = AdmissionRequest.builder()
                .id(UUID.randomUUID())
                .primaryAcuityTier(AcuityTier.TIER_3_ACUTE_STABLE)
                .effectiveAcuityTier(AcuityTier.TIER_1_CRITICAL)
                .requestedAt(now.minusMinutes(5))
                .build();

        // Request B: primary Tier 2, effective Tier 2
        AdmissionRequest reqB = AdmissionRequest.builder()
                .id(UUID.randomUUID())
                .primaryAcuityTier(AcuityTier.TIER_2_ACUTE_URGENT)
                .effectiveAcuityTier(AcuityTier.TIER_2_ACUTE_URGENT)
                .requestedAt(now.minusMinutes(20))
                .build();

        when(admissionRequestRepository.findByStatus(AdmissionStatus.BED_REQUESTED))
                .thenReturn(List.of(reqB, reqA));

        List<AdmissionRequest> queue = bmuService.getPrioritizedQueue();

        // reqA (effective Tier 1) must jump ahead of reqB (effective Tier 2)
        assertThat(queue).containsExactly(reqA, reqB);
    }

    @Test
    @DisplayName("getRecommendations returns solver recommendations")
    void testGetRecommendations() {
        UUID reqId = UUID.randomUUID();
        AdmissionRequest req = AdmissionRequest.builder().id(reqId).build();
        BmuAlgorithmConfig config = BmuAlgorithmConfig.builder().id(UUID.randomUUID()).build();
        BedRecommendation rec = BedRecommendation.builder().bedNumber("8A-01").score(85).build();

        when(admissionRequestRepository.findById(reqId)).thenReturn(Optional.of(req));
        when(configRepository.findAll()).thenReturn(List.of(config));
        when(solver.recommendBeds(req, config)).thenReturn(List.of(rec));

        List<BedRecommendation> recommendations = bmuService.getRecommendations(reqId);

        assertThat(recommendations).containsExactly(rec);
    }

    @Test
    @DisplayName("getRecommendations throws when request is not found")
    void testGetRecommendations_NotFound() {
        UUID reqId = UUID.randomUUID();
        when(admissionRequestRepository.findById(reqId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bmuService.getRecommendations(reqId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Admission request not found");
    }

    @Test
    @DisplayName("allocateBed successfully allocates bed when EMPTY_CLEANED")
    void testAllocateBed_Success() {
        UUID reqId = UUID.randomUUID();
        UUID bedId = UUID.randomUUID();
        Patient patient = Patient.builder().id(UUID.randomUUID()).name("Patient A").build();
        Ward ward = Ward.builder().name("Ward 8A").build();

        AdmissionRequest req = AdmissionRequest.builder()
                .id(reqId)
                .patient(patient)
                .status(AdmissionStatus.BED_REQUESTED)
                .build();

        Bed bed = Bed.builder()
                .id(bedId)
                .bedNumber("8A-01")
                .ward(ward)
                .status(BedStatus.EMPTY_CLEANED)
                .build();

        when(admissionRequestRepository.findById(reqId)).thenReturn(Optional.of(req));
        when(bedRepository.findById(bedId)).thenReturn(Optional.of(bed));
        when(admissionRequestRepository.save(req)).thenReturn(req);

        AdmissionRequest allocated = bmuService.allocateBed(reqId, bedId);

        assertThat(allocated.getStatus()).isEqualTo(AdmissionStatus.BED_ALLOCATED);
        assertThat(allocated.getAssignedBed()).isEqualTo(bed);
        assertThat(bed.getStatus()).isEqualTo(BedStatus.EMPTY_ASSIGNED);
        assertThat(bed.getCurrentPatient()).isEqualTo(patient);
        verify(bedRepository).save(bed);
        verify(admissionRequestRepository).save(req);
        verify(auditLogger).logAction(eq("bmu_coord"), eq("ALLOCATE_BED"), anyString(), anyString());
    }

    @Test
    @DisplayName("allocateBed throws when bed is not EMPTY_CLEANED")
    void testAllocateBed_NotCleaned() {
        UUID reqId = UUID.randomUUID();
        UUID bedId = UUID.randomUUID();
        AdmissionRequest req = AdmissionRequest.builder().id(reqId).build();
        Bed bed = Bed.builder()
                .id(bedId)
                .bedNumber("8A-01")
                .status(BedStatus.OCCUPIED_TAKEN)
                .build();

        when(admissionRequestRepository.findById(reqId)).thenReturn(Optional.of(req));
        when(bedRepository.findById(bedId)).thenReturn(Optional.of(bed));

        assertThatThrownBy(() -> bmuService.allocateBed(reqId, bedId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not EMPTY_CLEANED");

        verify(admissionRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("allocateBed throws when request or bed is not found")
    void testAllocateBed_NotFound() {
        UUID reqId = UUID.randomUUID();
        UUID bedId = UUID.randomUUID();

        when(admissionRequestRepository.findById(reqId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> bmuService.allocateBed(reqId, bedId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Admission request not found");

        when(admissionRequestRepository.findById(reqId)).thenReturn(Optional.of(AdmissionRequest.builder().id(reqId).build()));
        when(bedRepository.findById(bedId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> bmuService.allocateBed(reqId, bedId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Bed not found");
    }

    @Test
    @DisplayName("getInventory returns wards ordered by level and name")
    void testGetInventory() {
        Ward ward = Ward.builder().id(UUID.randomUUID()).name("Ward 8A").level(8).build();
        when(wardRepository.findAllByOrderByLevelAscNameAsc()).thenReturn(List.of(ward));

        List<WardDto> inventory = bmuService.getInventory();

        assertThat(inventory).hasSize(1);
        assertThat(inventory.get(0).wardCode()).isEqualTo("8A");
        assertThat(inventory.get(0).levelNumber()).isEqualTo(8);
    }

    @Test
    @DisplayName("getOrCreateConfig creates default config when none exists")
    void testGetOrCreateConfig_CreatesDefault() {
        when(configRepository.findAll()).thenReturn(List.of());
        BmuAlgorithmConfig saved = BmuAlgorithmConfig.builder().id(UUID.randomUUID()).weightSpecialtyCluster(40).build();
        when(configRepository.save(any(BmuAlgorithmConfig.class))).thenReturn(saved);

        BmuAlgorithmConfig result = bmuService.getOrCreateConfig();

        assertThat(result.getWeightSpecialtyCluster()).isEqualTo(40);
        verify(configRepository).save(any(BmuAlgorithmConfig.class));
    }

    @Test
    @DisplayName("updateConfig updates and saves config")
    void testUpdateConfig() {
        BmuAlgorithmConfig existing = BmuAlgorithmConfig.builder()
                .id(UUID.randomUUID())
                .weightSpecialtyCluster(40)
                .weightConsolidation(30)
                .weightFallRiskStation(15)
                .batchHoldingWardThreshold(3)
                .build();

        when(configRepository.findAll()).thenReturn(List.of(existing));
        when(configRepository.save(existing)).thenReturn(existing);

        BmuConfigUpdateRequest updateReq = BmuConfigUpdateRequest.builder()
                .weightSpecialtyCluster(50)
                .weightConsolidation(25)
                .weightFallRiskStation(20)
                .batchHoldingWardThreshold(4)
                .build();

        BmuAlgorithmConfig updated = bmuService.updateConfig(updateReq);

        assertThat(updated.getWeightSpecialtyCluster()).isEqualTo(50);
        assertThat(updated.getWeightConsolidation()).isEqualTo(25);
        assertThat(updated.getWeightFallRiskStation()).isEqualTo(20);
        assertThat(updated.getBatchHoldingWardThreshold()).isEqualTo(4);
        verify(auditLogger).logAction(eq("bmu_coord"), eq("UPDATE_BMU_CONFIG"), anyString(), anyString());
    }

    @Test
    @DisplayName("referToSisterHospital dispatches referral and updates admission request")
    void testReferToSisterHospital() {
        UUID reqId = UUID.randomUUID();
        Patient patient = Patient.builder().id(UUID.randomUUID()).name("Patient B").build();
        AdmissionRequest req = AdmissionRequest.builder().id(reqId).patient(patient).build();

        SisterHospitalReferralResponse response = SisterHospitalReferralResponse.builder()
                .referralId("REF-OCH-1234")
                .destinationFacility("Outram Community Hospital (OCH)")
                .status("ACCEPTED_30MIN_SLA")
                .build();

        when(admissionRequestRepository.findById(reqId)).thenReturn(Optional.of(req));
        when(sisterHospitalGateway.referPatient(patient, req, "OCH")).thenReturn(response);
        when(admissionRequestRepository.save(req)).thenReturn(req);

        SisterHospitalReferralResponse result = bmuService.referToSisterHospital(reqId, "OCH");

        assertThat(result.getReferralId()).isEqualTo("REF-OCH-1234");
        assertThat(req.isDiversionRecommended()).isTrue();
        assertThat(req.getSisterHospitalReferralId()).isEqualTo("REF-OCH-1234");
        verify(admissionRequestRepository).save(req);
        verify(auditLogger).logAction(eq("bmu_coord"), eq("DIVERSION_REFERRAL"), anyString(), anyString());
    }

    @Test
    @DisplayName("referToSisterHospital throws when request not found")
    void testReferToSisterHospital_NotFound() {
        UUID reqId = UUID.randomUUID();
        when(admissionRequestRepository.findById(reqId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bmuService.referToSisterHospital(reqId, "OCH"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Admission request not found");
    }

    @Test
    @DisplayName("requestReconciliation: Sets reconciliationRequested to true and emits REQUEST_CLINICAL_RECONCILIATION audit event")
    void testRequestReconciliation_SetsReconciliationRequestedAndLogsAudit() {
        UUID reqId = UUID.randomUUID();
        AdmissionRequest req = AdmissionRequest.builder()
                .id(reqId)
                .primaryAcuityTier(AcuityTier.TIER_3_ACUTE_STABLE)
                .effectiveAcuityTier(AcuityTier.TIER_2_ACUTE_URGENT)
                .isDiscordant(true)
                .reconciliationRequested(false)
                .build();

        when(admissionRequestRepository.findById(reqId)).thenReturn(Optional.of(req));
        when(admissionRequestRepository.save(req)).thenReturn(req);

        AdmissionRequest result = bmuService.requestReconciliation(reqId);

        assertThat(result.getReconciliationRequested()).isTrue();
        verify(admissionRequestRepository).save(req);
        verify(auditLogger).logAction(eq("bmu_coord"), eq("REQUEST_CLINICAL_RECONCILIATION"), eq("AdmissionRequest:" + reqId), anyString());
    }

    @Test
    @DisplayName("assignAdmittingCluster: Sets authoritative cluster and emits ASSIGN_ADMITTING_CLUSTER audit event")
    void testAssignAdmittingCluster_UpdatesClusterAndLogsAudit() {
        UUID reqId = UUID.randomUUID();
        AdmissionRequest req = AdmissionRequest.builder()
                .id(reqId)
                .primaryAcuityTier(AcuityTier.TIER_3_ACUTE_STABLE)
                .effectiveAcuityTier(AcuityTier.TIER_3_ACUTE_STABLE)
                .build();

        when(admissionRequestRepository.findById(reqId)).thenReturn(Optional.of(req));
        when(admissionRequestRepository.save(req)).thenReturn(req);

        AdmissionRequest result = bmuService.assignAdmittingCluster(reqId, SpecialtyCluster.CARDIOLOGY);

        assertThat(result.getAdmittingSpecialtyCluster()).isEqualTo(SpecialtyCluster.CARDIOLOGY);
        verify(admissionRequestRepository).save(req);
        verify(auditLogger).logAction(eq("bmu_coord"), eq("ASSIGN_ADMITTING_CLUSTER"), eq("AdmissionRequest:" + reqId), anyString());
    }

    @Test
    @DisplayName("allocateBed: Reallocating a tentatively allocated bed reverts the old bed to EMPTY_CLEANED")
    void testAllocateBed_DynamicReallocation_RevertsOldBedToEmptyCleaned() {
        UUID reqId = UUID.randomUUID();
        Patient patient = Patient.builder().id(UUID.randomUUID()).name("Reallocation Patient").build();

        Ward ward = Ward.builder().id(UUID.randomUUID()).name("Ward 8A").build();

        UUID oldBedId = UUID.randomUUID();
        Bed oldBed = Bed.builder()
                .id(oldBedId)
                .bedNumber("8A-01")
                .ward(ward)
                .status(BedStatus.EMPTY_ASSIGNED)
                .currentPatient(patient)
                .build();

        AdmissionRequest req = AdmissionRequest.builder()
                .id(reqId)
                .patient(patient)
                .status(AdmissionStatus.BED_ALLOCATED)
                .assignedBed(oldBed)
                .build();

        UUID newBedId = UUID.randomUUID();
        Bed newBed = Bed.builder()
                .id(newBedId)
                .bedNumber("8A-02")
                .ward(ward)
                .status(BedStatus.EMPTY_CLEANED)
                .build();

        when(admissionRequestRepository.findById(reqId)).thenReturn(Optional.of(req));
        when(bedRepository.findById(newBedId)).thenReturn(Optional.of(newBed));
        when(admissionRequestRepository.save(req)).thenReturn(req);

        AdmissionRequest result = bmuService.allocateBed(reqId, newBedId);

        assertThat(result.getAssignedBed()).isEqualTo(newBed);
        assertThat(newBed.getStatus()).isEqualTo(BedStatus.EMPTY_ASSIGNED);
        assertThat(newBed.getCurrentPatient()).isEqualTo(patient);

        // Verify old bed was freed back to EMPTY_CLEANED without ghost reservation
        assertThat(oldBed.getStatus()).isEqualTo(BedStatus.EMPTY_CLEANED);
        assertThat(oldBed.getCurrentPatient()).isNull();
        verify(bedRepository).save(oldBed);
        verify(bedRepository).save(newBed);
    }

    @Test
    @DisplayName("allocateBed throws SafetyInvariantViolationException (422) when gender cohorting is violated in multi-bed ward")
    void testAllocateBed_SafetyInvariant_GenderCohorting() {
        UUID reqId = UUID.randomUUID();
        UUID bedId = UUID.randomUUID();
        Patient malePatient = Patient.builder().id(UUID.randomUUID()).name("Male Patient").gender(Gender.MALE).build();
        Ward femaleWard = Ward.builder().id(UUID.randomUUID()).name("Ward 8B").capacity(4).lockedGender(Gender.FEMALE).wardClass(WardClass.B2).build();
        Bed bed = Bed.builder().id(bedId).bedNumber("8B-01").ward(femaleWard).status(BedStatus.EMPTY_CLEANED).hasTelemetry(true).build();
        AdmissionRequest req = AdmissionRequest.builder().id(reqId).patient(malePatient).requestedWardClass(WardClass.B2).status(AdmissionStatus.BED_REQUESTED).build();

        when(admissionRequestRepository.findById(reqId)).thenReturn(Optional.of(req));
        when(bedRepository.findById(bedId)).thenReturn(Optional.of(bed));

        assertThatThrownBy(() -> bmuService.allocateBed(reqId, bedId))
                .isInstanceOf(com.hospital.admissions.exception.SafetyInvariantViolationException.class)
                .hasMessageContaining("Biological gender cohorting violation");
    }

    @Test
    @DisplayName("allocateBed throws SafetyInvariantViolationException (422) when airborne infection is placed in non-negative pressure ward")
    void testAllocateBed_SafetyInvariant_NegativePressureIsolation() {
        UUID reqId = UUID.randomUUID();
        UUID bedId = UUID.randomUUID();
        Patient respiratoryPatient = Patient.builder().id(UUID.randomUUID()).name("Resp Patient").gender(Gender.FEMALE).infectionStatus(InfectionStatus.RESPIRATORY).build();
        Ward standardWard = Ward.builder().id(UUID.randomUUID()).name("Ward 8A").capacity(4).isNegativePressure(false).wardClass(WardClass.B2).build();
        Bed bed = Bed.builder().id(bedId).bedNumber("8A-01").ward(standardWard).status(BedStatus.EMPTY_CLEANED).hasTelemetry(true).build();
        AdmissionRequest req = AdmissionRequest.builder().id(reqId).patient(respiratoryPatient).requestedWardClass(WardClass.B2).status(AdmissionStatus.BED_REQUESTED).build();

        when(admissionRequestRepository.findById(reqId)).thenReturn(Optional.of(req));
        when(bedRepository.findById(bedId)).thenReturn(Optional.of(bed));

        assertThatThrownBy(() -> bmuService.allocateBed(reqId, bedId))
                .isInstanceOf(com.hospital.admissions.exception.SafetyInvariantViolationException.class)
                .hasMessageContaining("negative pressure isolation");
    }

    @Test
    @DisplayName("allocateBed throws IllegalArgumentException (400) when ward class upgrade has no structured reason code")
    void testAllocateBed_OperationalConstraint_WardClassMismatchWithoutReason() {
        UUID reqId = UUID.randomUUID();
        UUID bedId = UUID.randomUUID();
        Patient patient = Patient.builder().id(UUID.randomUUID()).name("Subsidized Patient").gender(Gender.FEMALE).infectionStatus(InfectionStatus.NON_INFECTIOUS).build();
        Ward b2Ward = Ward.builder().id(UUID.randomUUID()).name("Ward 8A").capacity(4).wardClass(WardClass.B2).build();
        Bed bed = Bed.builder().id(bedId).bedNumber("8A-01").ward(b2Ward).status(BedStatus.EMPTY_CLEANED).hasTelemetry(true).build();
        AdmissionRequest req = AdmissionRequest.builder().id(reqId).patient(patient).requestedWardClass(WardClass.C).status(AdmissionStatus.BED_REQUESTED).build();

        when(admissionRequestRepository.findById(reqId)).thenReturn(Optional.of(req));
        when(bedRepository.findById(bedId)).thenReturn(Optional.of(bed));

        assertThatThrownBy(() -> bmuService.allocateBed(reqId, bedId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Operational constraint override requires a valid structured reason code");
    }

    @Test
    @DisplayName("allocateBed throws IllegalArgumentException (400) when telemetry is required on non-telemetry bed without reason code")
    void testAllocateBed_OperationalConstraint_TelemetryRequiredWithoutReason() {
        UUID reqId = UUID.randomUUID();
        UUID bedId = UUID.randomUUID();
        Patient patient = Patient.builder().id(UUID.randomUUID()).name("Telemetry Patient").gender(Gender.FEMALE).needsTelemetry(true).infectionStatus(InfectionStatus.NON_INFECTIOUS).build();
        Ward b2Ward = Ward.builder().id(UUID.randomUUID()).name("Ward 8A").capacity(4).wardClass(WardClass.B2).build();
        Bed bed = Bed.builder().id(bedId).bedNumber("8A-01").ward(b2Ward).status(BedStatus.EMPTY_CLEANED).hasTelemetry(false).build();
        AdmissionRequest req = AdmissionRequest.builder().id(reqId).patient(patient).requestedWardClass(WardClass.B2).primaryTelemetry(true).status(AdmissionStatus.BED_REQUESTED).build();

        when(admissionRequestRepository.findById(reqId)).thenReturn(Optional.of(req));
        when(bedRepository.findById(bedId)).thenReturn(Optional.of(bed));

        assertThatThrownBy(() -> bmuService.allocateBed(reqId, bedId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Operational constraint override requires a valid structured reason code");
    }

    @Test
    @DisplayName("allocateBed succeeds with OVERRIDE_ALLOCATION audit when valid reason code is supplied for ward class upgrade")
    void testAllocateBed_OperationalConstraint_WardClassUpgrade_SuccessWithReason() {
        UUID reqId = UUID.randomUUID();
        UUID bedId = UUID.randomUUID();
        Patient patient = Patient.builder().id(UUID.randomUUID()).name("Subsidized Patient").gender(Gender.FEMALE).infectionStatus(InfectionStatus.NON_INFECTIOUS).build();
        Ward b2Ward = Ward.builder().id(UUID.randomUUID()).name("Ward 8A").capacity(4).wardClass(WardClass.B2).build();
        Bed bed = Bed.builder().id(bedId).bedNumber("8A-01").ward(b2Ward).status(BedStatus.EMPTY_CLEANED).hasTelemetry(true).build();
        AdmissionRequest req = AdmissionRequest.builder().id(reqId).patient(patient).requestedWardClass(WardClass.C).status(AdmissionStatus.BED_REQUESTED).build();

        when(admissionRequestRepository.findById(reqId)).thenReturn(Optional.of(req));
        when(bedRepository.findById(bedId)).thenReturn(Optional.of(bed));
        when(admissionRequestRepository.save(req)).thenReturn(req);

        AdmissionRequest allocated = bmuService.allocateBed(reqId, bedId, 2, 70.0, "GOVERNMENT_SUBSIDY_CLASS_UPGRADE");

        assertThat(allocated.getStatus()).isEqualTo(AdmissionStatus.BED_ALLOCATED);
        assertThat(allocated.getOverrideReasonCode()).isEqualTo("GOVERNMENT_SUBSIDY_CLASS_UPGRADE");
        assertThat(allocated.getIsRecommendationAccepted()).isFalse();
        verify(auditLogger).logAction(eq("bmu_coord"), eq("OVERRIDE_ALLOCATION"), eq("AdmissionRequest:" + reqId), contains("GOVERNMENT_SUBSIDY_CLASS_UPGRADE"));
    }

    @Test
    @DisplayName("deallocateBed: Reverts EMPTY_ASSIGNED bed to EMPTY_CLEANED and resets request to BED_REQUESTED")
    void testDeallocateBed_Success() {
        UUID reqId = UUID.randomUUID();
        UUID bedId = UUID.randomUUID();
        Patient patient = Patient.builder().id(UUID.randomUUID()).name("Patient").build();
        Bed bed = Bed.builder().id(bedId).bedNumber("8A-01").status(BedStatus.EMPTY_ASSIGNED).currentPatient(patient).build();
        AdmissionRequest req = AdmissionRequest.builder().id(reqId).status(AdmissionStatus.BED_ALLOCATED).assignedBed(bed).patient(patient).build();

        when(admissionRequestRepository.findById(reqId)).thenReturn(Optional.of(req));
        when(admissionRequestRepository.save(req)).thenReturn(req);

        AdmissionRequest result = bmuService.deallocateBed(reqId);

        assertThat(result.getStatus()).isEqualTo(AdmissionStatus.BED_REQUESTED);
        assertThat(result.getAssignedBed()).isNull();
        assertThat(bed.getStatus()).isEqualTo(BedStatus.EMPTY_CLEANED);
        assertThat(bed.getCurrentPatient()).isNull();
        verify(bedRepository).save(bed);
        verify(admissionRequestRepository).save(req);
        verify(auditLogger).logAction(eq("bmu_coord"), eq("DEALLOCATE_BED"), eq("AdmissionRequest:" + reqId), anyString());
    }

    @Test
    @DisplayName("deallocateBed: Throws IllegalStateException when patient has already physically arrived")
    void testDeallocateBed_ThrowsConflict_WhenPatientAlreadyArrived() {
        UUID reqId = UUID.randomUUID();
        UUID bedId = UUID.randomUUID();
        Patient patient = Patient.builder().id(UUID.randomUUID()).name("Patient").build();
        Bed bed = Bed.builder().id(bedId).bedNumber("8A-01").status(BedStatus.OCCUPIED_TAKEN).currentPatient(patient).build();
        AdmissionRequest req = AdmissionRequest.builder().id(reqId).status(AdmissionStatus.ADMITTED_INPATIENT).assignedBed(bed).patient(patient).build();

        when(admissionRequestRepository.findById(reqId)).thenReturn(Optional.of(req));

        assertThatThrownBy(() -> bmuService.deallocateBed(reqId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot deallocate");
    }

    @Test
    @DisplayName("confirmArrival: Transitions EMPTY_ASSIGNED bed to OCCUPIED_TAKEN and admission to ADMITTED_INPATIENT")
    void testConfirmArrival_Success() {
        UUID bedId = UUID.randomUUID();
        Patient patient = Patient.builder().id(UUID.randomUUID()).name("Patient").build();
        Bed bed = Bed.builder().id(bedId).bedNumber("8A-01").status(BedStatus.EMPTY_ASSIGNED).currentPatient(patient).build();
        AdmissionRequest req = AdmissionRequest.builder().id(UUID.randomUUID()).status(AdmissionStatus.BED_ALLOCATED).assignedBed(bed).build();

        when(bedRepository.findById(bedId)).thenReturn(Optional.of(bed));
        when(admissionRequestRepository.findByAssignedBed_Id(bedId)).thenReturn(Optional.of(req));
        when(bedRepository.save(bed)).thenReturn(bed);

        Bed result = bmuService.confirmArrival(bedId);

        assertThat(result.getStatus()).isEqualTo(BedStatus.OCCUPIED_TAKEN);
        assertThat(req.getStatus()).isEqualTo(AdmissionStatus.ADMITTED_INPATIENT);
        verify(bedRepository).save(bed);
        verify(admissionRequestRepository).save(req);
        verify(auditLogger).logAction(eq("bmu_coord"), eq("CONFIRM_PATIENT_ARRIVAL"), eq("Bed:" + bedId), anyString());
    }

    @Test
    @DisplayName("vacateBed: Transitions OCCUPIED_TAKEN bed to EMPTY_PENDING_CLEANING and starts cleaning SLA")
    void testVacateBed_Success() {
        UUID bedId = UUID.randomUUID();
        Patient patient = Patient.builder().id(UUID.randomUUID()).name("Patient").build();
        Bed bed = Bed.builder().id(bedId).bedNumber("8A-01").status(BedStatus.OCCUPIED_TAKEN).currentPatient(patient).build();
        AdmissionRequest req = AdmissionRequest.builder().id(UUID.randomUUID()).status(AdmissionStatus.ADMITTED_INPATIENT).assignedBed(bed).build();

        when(bedRepository.findById(bedId)).thenReturn(Optional.of(bed));
        when(admissionRequestRepository.findByAssignedBed_Id(bedId)).thenReturn(Optional.of(req));
        when(bedRepository.save(bed)).thenReturn(bed);

        Bed result = bmuService.vacateBed(bedId);

        assertThat(result.getStatus()).isEqualTo(BedStatus.EMPTY_PENDING_CLEANING);
        assertThat(result.getCurrentPatient()).isNull();
        assertThat(result.getCleaningStartedAt()).isNotNull();
        assertThat(req.getStatus()).isEqualTo(AdmissionStatus.DISCHARGED);
        assertThat(req.getAssignedBed()).isNull();
        verify(bedRepository).save(bed);
        verify(admissionRequestRepository).save(req);
        verify(auditLogger).logAction(eq("bmu_coord"), eq("VACATE_BED"), eq("Bed:" + bedId), anyString());
    }

    @Test
    @DisplayName("signOffCleaning: Transitions EMPTY_PENDING_CLEANING bed to EMPTY_CLEANED and triggers All-Clean Ward Reset")
    void testSignOffCleaning_TriggersAllCleanWardReset_WhenAllBedsClean() {
        UUID wardId = UUID.randomUUID();
        Ward ward = Ward.builder().id(wardId).name("Ward 8A").capacity(2).lockedGender(Gender.MALE).isHoldingWard(true).build();
        UUID bed1Id = UUID.randomUUID();
        Bed bed1 = Bed.builder().id(bed1Id).bedNumber("8A-01").ward(ward).status(BedStatus.EMPTY_PENDING_CLEANING).cleaningStartedAt(LocalDateTime.now().minusMinutes(25)).build();
        Bed bed2 = Bed.builder().id(UUID.randomUUID()).bedNumber("8A-02").ward(ward).status(BedStatus.EMPTY_CLEANED).build();

        when(bedRepository.findById(bed1Id)).thenReturn(Optional.of(bed1));
        when(bedRepository.findByWard_Id(wardId)).thenReturn(List.of(bed1, bed2));
        when(bedRepository.save(bed1)).thenReturn(bed1);
        when(wardRepository.save(ward)).thenReturn(ward);

        Bed result = bmuService.signOffCleaning(bed1Id);

        assertThat(result.getStatus()).isEqualTo(BedStatus.EMPTY_CLEANED);
        assertThat(result.getLastCleanedAt()).isNotNull();
        assertThat(result.getCleaningStartedAt()).isNull();

        // Verify All-Clean Ward Reset triggered
        assertThat(ward.getLockedGender()).isNull();
        assertThat(ward.getLockedInfectionStatus()).isNull();
        assertThat(ward.isHoldingWard()).isFalse();
        verify(wardRepository).save(ward);
        verify(auditLogger).logAction(eq("bmu_coord"), eq("SIGN_OFF_CLEANING"), eq("Bed:" + bed1Id), anyString());
        verify(auditLogger).logAction(eq("bmu_coord"), eq("ALL_CLEAN_WARD_RESET"), eq("Ward:" + wardId), anyString());
    }

    @Test
    @DisplayName("getBatchSuggestions: Detects surge cluster >= 3 and pairs with all-White flex ward")
    void testGetBatchSuggestions_DetectsSurgeClusters_AndPairsWithFlexWard() {
        LocalDateTime now = LocalDateTime.now();
        Patient p1 = Patient.builder().id(UUID.randomUUID()).name("P1").gender(Gender.MALE).infectionStatus(InfectionStatus.NON_INFECTIOUS).build();
        Patient p2 = Patient.builder().id(UUID.randomUUID()).name("P2").gender(Gender.MALE).infectionStatus(InfectionStatus.NON_INFECTIOUS).build();
        Patient p3 = Patient.builder().id(UUID.randomUUID()).name("P3").gender(Gender.MALE).infectionStatus(InfectionStatus.NON_INFECTIOUS).build();

        AdmissionRequest r1 = AdmissionRequest.builder().id(UUID.randomUUID()).patient(p1).requestedWardClass(WardClass.B2).status(AdmissionStatus.BED_REQUESTED).requestedAt(now.minusMinutes(30)).build();
        AdmissionRequest r2 = AdmissionRequest.builder().id(UUID.randomUUID()).patient(p2).requestedWardClass(WardClass.B2).status(AdmissionStatus.BED_REQUESTED).requestedAt(now.minusMinutes(20)).build();
        AdmissionRequest r3 = AdmissionRequest.builder().id(UUID.randomUUID()).patient(p3).requestedWardClass(WardClass.B2).status(AdmissionStatus.BED_REQUESTED).requestedAt(now.minusMinutes(10)).build();

        when(admissionRequestRepository.findByStatus(AdmissionStatus.BED_REQUESTED)).thenReturn(List.of(r1, r2, r3));

        UUID flexWardId = UUID.randomUUID();
        Ward flexWard = Ward.builder().id(flexWardId).name("Flex Ward 9C").wardClass(WardClass.B2).capacity(4).build();
        Bed b1 = Bed.builder().id(UUID.randomUUID()).bedNumber("9C-01").ward(flexWard).status(BedStatus.EMPTY_CLEANED).build();
        Bed b2 = Bed.builder().id(UUID.randomUUID()).bedNumber("9C-02").ward(flexWard).status(BedStatus.EMPTY_CLEANED).build();
        Bed b3 = Bed.builder().id(UUID.randomUUID()).bedNumber("9C-03").ward(flexWard).status(BedStatus.EMPTY_CLEANED).build();
        Bed b4 = Bed.builder().id(UUID.randomUUID()).bedNumber("9C-04").ward(flexWard).status(BedStatus.EMPTY_CLEANED).build();

        when(wardRepository.findAll()).thenReturn(List.of(flexWard));
        when(bedRepository.findByWard_Id(flexWardId)).thenReturn(List.of(b1, b2, b3, b4));

        List<com.hospital.admissions.dto.BatchSuggestion> suggestions = bmuService.getBatchSuggestions();

        assertThat(suggestions).hasSize(1);
        com.hospital.admissions.dto.BatchSuggestion suggestion = suggestions.get(0);
        assertThat(suggestion.getTargetWardId()).isEqualTo(flexWardId);
        assertThat(suggestion.getAdmissionRequestIds()).containsExactly(r1.getId(), r2.getId(), r3.getId()); // FIFO ordered
        assertThat(suggestion.getCommonWardClass()).isEqualTo(WardClass.B2);
        assertThat(suggestion.getCommonGender()).isEqualTo(Gender.MALE);
    }

    @Test
    @DisplayName("approveBatchHoldingWard: Atomically allocates cluster, locks ward cohort, and preserves residual clean beds")
    void testApproveBatchHoldingWard_AtomicallyAllocatesAndLocksCohort() {
        LocalDateTime now = LocalDateTime.now();
        Patient p1 = Patient.builder().id(UUID.randomUUID()).name("P1").gender(Gender.MALE).infectionStatus(InfectionStatus.NON_INFECTIOUS).build();
        Patient p2 = Patient.builder().id(UUID.randomUUID()).name("P2").gender(Gender.MALE).infectionStatus(InfectionStatus.NON_INFECTIOUS).build();
        Patient p3 = Patient.builder().id(UUID.randomUUID()).name("P3").gender(Gender.MALE).infectionStatus(InfectionStatus.NON_INFECTIOUS).build();

        AdmissionRequest r1 = AdmissionRequest.builder().id(UUID.randomUUID()).patient(p1).requestedWardClass(WardClass.B2).status(AdmissionStatus.BED_REQUESTED).requestedAt(now.minusMinutes(30)).build();
        AdmissionRequest r2 = AdmissionRequest.builder().id(UUID.randomUUID()).patient(p2).requestedWardClass(WardClass.B2).status(AdmissionStatus.BED_REQUESTED).requestedAt(now.minusMinutes(20)).build();
        AdmissionRequest r3 = AdmissionRequest.builder().id(UUID.randomUUID()).patient(p3).requestedWardClass(WardClass.B2).status(AdmissionStatus.BED_REQUESTED).requestedAt(now.minusMinutes(10)).build();

        UUID flexWardId = UUID.randomUUID();
        Ward flexWard = Ward.builder().id(flexWardId).name("Flex Ward 9C").wardClass(WardClass.B2).capacity(4).build();
        Bed b1 = Bed.builder().id(UUID.randomUUID()).bedNumber("9C-01").ward(flexWard).status(BedStatus.EMPTY_CLEANED).build();
        Bed b2 = Bed.builder().id(UUID.randomUUID()).bedNumber("9C-02").ward(flexWard).status(BedStatus.EMPTY_CLEANED).build();
        Bed b3 = Bed.builder().id(UUID.randomUUID()).bedNumber("9C-03").ward(flexWard).status(BedStatus.EMPTY_CLEANED).build();
        Bed b4 = Bed.builder().id(UUID.randomUUID()).bedNumber("9C-04").ward(flexWard).status(BedStatus.EMPTY_CLEANED).build();

        when(wardRepository.findById(flexWardId)).thenReturn(Optional.of(flexWard));
        when(bedRepository.findByWard_Id(flexWardId)).thenReturn(List.of(b1, b2, b3, b4));
        when(admissionRequestRepository.findById(r1.getId())).thenReturn(Optional.of(r1));
        when(admissionRequestRepository.findById(r2.getId())).thenReturn(Optional.of(r2));
        when(admissionRequestRepository.findById(r3.getId())).thenReturn(Optional.of(r3));

        com.hospital.admissions.dto.BatchApprovalRequest approval = com.hospital.admissions.dto.BatchApprovalRequest.builder()
                .targetWardId(flexWardId)
                .admissionRequestIds(List.of(r1.getId(), r2.getId(), r3.getId()))
                .build();

        List<AdmissionRequest> allocated = bmuService.approveBatchHoldingWard(approval);

        assertThat(allocated).hasSize(3);
        assertThat(r1.getStatus()).isEqualTo(AdmissionStatus.BED_ALLOCATED);
        assertThat(r2.getStatus()).isEqualTo(AdmissionStatus.BED_ALLOCATED);
        assertThat(r3.getStatus()).isEqualTo(AdmissionStatus.BED_ALLOCATED);

        // First 3 beds assigned
        assertThat(b1.getStatus()).isEqualTo(BedStatus.EMPTY_ASSIGNED);
        assertThat(b2.getStatus()).isEqualTo(BedStatus.EMPTY_ASSIGNED);
        assertThat(b3.getStatus()).isEqualTo(BedStatus.EMPTY_ASSIGNED);

        // Residual 4th bed remains clean
        assertThat(b4.getStatus()).isEqualTo(BedStatus.EMPTY_CLEANED);

        // Ward cohort locked
        assertThat(flexWard.getLockedGender()).isEqualTo(Gender.MALE);
        assertThat(flexWard.getLockedInfectionStatus()).isEqualTo(InfectionStatus.NON_INFECTIOUS);
        assertThat(flexWard.isHoldingWard()).isTrue();

        verify(wardRepository).save(flexWard);
        verify(auditLogger).logAction(eq("bmu_coord"), eq("APPROVE_BATCH_HOLDING_WARD"), eq("Ward:" + flexWardId), anyString());
    }

    @Test
    @DisplayName("getBatchSuggestions: Greedy FIFO dwell slicing prioritizes earliest requestedAt when cluster exceeds capacity")
    void testGetBatchSuggestions_GreedyFifoDwellSlicing_WhenClusterExceedsCapacity() {
        LocalDateTime now = LocalDateTime.now();
        Patient p1 = Patient.builder().id(UUID.randomUUID()).name("P1").gender(Gender.FEMALE).infectionStatus(InfectionStatus.NON_INFECTIOUS).build();
        Patient p2 = Patient.builder().id(UUID.randomUUID()).name("P2").gender(Gender.FEMALE).infectionStatus(InfectionStatus.NON_INFECTIOUS).build();
        Patient p3 = Patient.builder().id(UUID.randomUUID()).name("P3").gender(Gender.FEMALE).infectionStatus(InfectionStatus.NON_INFECTIOUS).build();
        Patient p4 = Patient.builder().id(UUID.randomUUID()).name("P4").gender(Gender.FEMALE).infectionStatus(InfectionStatus.NON_INFECTIOUS).build();

        // r4 requested 40m ago, r1 30m ago, r2 20m ago, r3 10m ago
        AdmissionRequest r1 = AdmissionRequest.builder().id(UUID.randomUUID()).patient(p1).requestedWardClass(WardClass.B2).status(AdmissionStatus.BED_REQUESTED).requestedAt(now.minusMinutes(30)).build();
        AdmissionRequest r2 = AdmissionRequest.builder().id(UUID.randomUUID()).patient(p2).requestedWardClass(WardClass.B2).status(AdmissionStatus.BED_REQUESTED).requestedAt(now.minusMinutes(20)).build();
        AdmissionRequest r3 = AdmissionRequest.builder().id(UUID.randomUUID()).patient(p3).requestedWardClass(WardClass.B2).status(AdmissionStatus.BED_REQUESTED).requestedAt(now.minusMinutes(10)).build();
        AdmissionRequest r4 = AdmissionRequest.builder().id(UUID.randomUUID()).patient(p4).requestedWardClass(WardClass.B2).status(AdmissionStatus.BED_REQUESTED).requestedAt(now.minusMinutes(40)).build();

        when(admissionRequestRepository.findByStatus(AdmissionStatus.BED_REQUESTED)).thenReturn(List.of(r1, r2, r3, r4));

        UUID flexWardId = UUID.randomUUID();
        Ward flexWard = Ward.builder().id(flexWardId).name("Flex Ward 9B").wardClass(WardClass.B2).capacity(3).build();
        Bed b1 = Bed.builder().id(UUID.randomUUID()).bedNumber("9B-01").ward(flexWard).status(BedStatus.EMPTY_CLEANED).build();
        Bed b2 = Bed.builder().id(UUID.randomUUID()).bedNumber("9B-02").ward(flexWard).status(BedStatus.EMPTY_CLEANED).build();
        Bed b3 = Bed.builder().id(UUID.randomUUID()).bedNumber("9B-03").ward(flexWard).status(BedStatus.EMPTY_CLEANED).build();

        when(wardRepository.findAll()).thenReturn(List.of(flexWard));
        when(bedRepository.findByWard_Id(flexWardId)).thenReturn(List.of(b1, b2, b3));

        List<com.hospital.admissions.dto.BatchSuggestion> suggestions = bmuService.getBatchSuggestions();

        assertThat(suggestions).hasSize(1);
        com.hospital.admissions.dto.BatchSuggestion suggestion = suggestions.get(0);
        assertThat(suggestion.getAdmissionRequestIds()).hasSize(3);
        // Greedy FIFO: r4 (40m), r1 (30m), r2 (20m). r3 (10m) left behind
        assertThat(suggestion.getAdmissionRequestIds()).containsExactly(r4.getId(), r1.getId(), r2.getId());
    }

    @Test
    @DisplayName("getCohortSwapSuggestions: Identifies blocked flex ward and candidate transfer bed in partially occupied ward")
    void testGetCohortSwapSuggestions_IdentifiesBlockedFlexWardAndCandidateBed() {
        // Blocked flex ward 9A with 4 beds: 1 bed EMPTY_ASSIGNED to a male B2 patient, 3 beds EMPTY_CLEANED
        UUID flexWardId = UUID.randomUUID();
        Ward flexWard = Ward.builder().id(flexWardId).name("Flex Ward 9A").wardClass(WardClass.B2).capacity(4).lockedGender(Gender.MALE).build();
        UUID assignedBedId = UUID.randomUUID();
        Patient patient = Patient.builder().id(UUID.randomUUID()).name("John Doe").gender(Gender.MALE).infectionStatus(InfectionStatus.NON_INFECTIOUS).build();
        Bed assignedBed = Bed.builder().id(assignedBedId).bedNumber("9A-01").ward(flexWard).status(BedStatus.EMPTY_ASSIGNED).currentPatient(patient).build();
        Bed cleanBed1 = Bed.builder().id(UUID.randomUUID()).bedNumber("9A-02").ward(flexWard).status(BedStatus.EMPTY_CLEANED).build();
        Bed cleanBed2 = Bed.builder().id(UUID.randomUUID()).bedNumber("9A-03").ward(flexWard).status(BedStatus.EMPTY_CLEANED).build();
        Bed cleanBed3 = Bed.builder().id(UUID.randomUUID()).bedNumber("9A-04").ward(flexWard).status(BedStatus.EMPTY_CLEANED).build();

        UUID reqId = UUID.randomUUID();
        AdmissionRequest req = AdmissionRequest.builder()
                .id(reqId)
                .patient(patient)
                .requestedWardClass(WardClass.B2)
                .status(AdmissionStatus.BED_ALLOCATED)
                .assignedBed(assignedBed)
                .waitingInEd(true)
                .build();

        // Partially occupied ward 8A with 4 beds: 2 OCCUPIED_TAKEN (male), 1 EMPTY_CLEANED, 1 OCCUPIED_TAKEN
        UUID targetWardId = UUID.randomUUID();
        Ward targetWard = Ward.builder().id(targetWardId).name("Ward 8A").wardClass(WardClass.B2).capacity(4).lockedGender(Gender.MALE).build();
        UUID targetBedId = UUID.randomUUID();
        Bed targetBed = Bed.builder().id(targetBedId).bedNumber("8A-03").ward(targetWard).status(BedStatus.EMPTY_CLEANED).build();
        Bed occupiedBed = Bed.builder().id(UUID.randomUUID()).bedNumber("8A-01").ward(targetWard).status(BedStatus.OCCUPIED_TAKEN).build();

        when(wardRepository.findAll()).thenReturn(List.of(flexWard, targetWard));
        when(bedRepository.findByWard_Id(flexWardId)).thenReturn(List.of(assignedBed, cleanBed1, cleanBed2, cleanBed3));
        when(bedRepository.findByWard_Id(targetWardId)).thenReturn(List.of(occupiedBed, targetBed));
        when(admissionRequestRepository.findByAssignedBed_Id(assignedBedId)).thenReturn(Optional.of(req));

        List<com.hospital.admissions.dto.CohortSwapSuggestion> suggestions = bmuService.getCohortSwapSuggestions();

        assertThat(suggestions).hasSize(1);
        com.hospital.admissions.dto.CohortSwapSuggestion suggestion = suggestions.get(0);
        assertThat(suggestion.getAdmissionRequestId()).isEqualTo(reqId);
        assertThat(suggestion.getCurrentBedId()).isEqualTo(assignedBedId);
        assertThat(suggestion.getTargetBedId()).isEqualTo(targetBedId);
        assertThat(suggestion.getTargetWardName()).isEqualTo("Ward 8A");
        assertThat(suggestion.getUnlockedCapacityCount()).isEqualTo(4);
    }

    @Test
    @DisplayName("approveCohortSwap: Atomically transfers patient, resets flex bed, and triggers all-clean ward reset")
    void testApproveCohortSwap_Success() {
        UUID flexWardId = UUID.randomUUID();
        Ward flexWard = Ward.builder().id(flexWardId).name("Flex Ward 9A").capacity(4).lockedGender(Gender.MALE).isHoldingWard(true).build();
        UUID assignedBedId = UUID.randomUUID();
        Patient patient = Patient.builder().id(UUID.randomUUID()).name("John Doe").gender(Gender.MALE).build();
        Bed flexBed = Bed.builder().id(assignedBedId).bedNumber("9A-01").ward(flexWard).status(BedStatus.EMPTY_ASSIGNED).currentPatient(patient).build();
        Bed flexBed2 = Bed.builder().id(UUID.randomUUID()).bedNumber("9A-02").ward(flexWard).status(BedStatus.EMPTY_CLEANED).build();

        UUID targetWardId = UUID.randomUUID();
        Ward targetWard = Ward.builder().id(targetWardId).name("Ward 8A").capacity(4).lockedGender(Gender.MALE).build();
        UUID targetBedId = UUID.randomUUID();
        Bed targetBed = Bed.builder().id(targetBedId).bedNumber("8A-03").ward(targetWard).status(BedStatus.EMPTY_CLEANED).build();

        UUID reqId = UUID.randomUUID();
        AdmissionRequest req = AdmissionRequest.builder()
                .id(reqId)
                .patient(patient)
                .status(AdmissionStatus.BED_ALLOCATED)
                .assignedBed(flexBed)
                .waitingInEd(true)
                .build();

        when(admissionRequestRepository.findById(reqId)).thenReturn(Optional.of(req));
        when(bedRepository.findById(targetBedId)).thenReturn(Optional.of(targetBed));
        when(bedRepository.findByWard_Id(flexWardId)).thenReturn(List.of(flexBed, flexBed2));

        com.hospital.admissions.dto.CohortSwapApprovalRequest request = com.hospital.admissions.dto.CohortSwapApprovalRequest.builder()
                .admissionRequestId(reqId)
                .targetBedId(targetBedId)
                .build();

        AdmissionRequest result = bmuService.approveCohortSwap(request);

        assertThat(result.getAssignedBed()).isEqualTo(targetBed);
        assertThat(targetBed.getStatus()).isEqualTo(BedStatus.EMPTY_ASSIGNED);
        assertThat(targetBed.getCurrentPatient()).isEqualTo(patient);

        // Flex bed freed
        assertThat(flexBed.getStatus()).isEqualTo(BedStatus.EMPTY_CLEANED);
        assertThat(flexBed.getCurrentPatient()).isNull();

        // Flex ward cohort lock released
        assertThat(flexWard.getLockedGender()).isNull();
        assertThat(flexWard.isHoldingWard()).isFalse();

        verify(auditLogger).logAction(eq("bmu_coord"), eq("APPROVE_COHORT_SWAP"), contains(reqId.toString()), anyString());
    }

    @Test
    @DisplayName("approveCohortSwap: Rejects with 409 Conflict if patient has departed ED")
    void testApproveCohortSwap_Conflict_WhenPatientDepartedEd() {
        UUID reqId = UUID.randomUUID();
        UUID targetBedId = UUID.randomUUID();
        Patient patient = Patient.builder().id(UUID.randomUUID()).name("John Doe").build();
        Bed flexBed = Bed.builder().id(UUID.randomUUID()).bedNumber("9A-01").status(BedStatus.EMPTY_ASSIGNED).currentPatient(patient).build();
        Bed targetBed = Bed.builder().id(targetBedId).bedNumber("8A-03").status(BedStatus.EMPTY_CLEANED).build();

        AdmissionRequest req = AdmissionRequest.builder()
                .id(reqId)
                .patient(patient)
                .assignedBed(flexBed)
                .waitingInEd(false) // Patient departed ED
                .build();

        when(admissionRequestRepository.findById(reqId)).thenReturn(Optional.of(req));

        com.hospital.admissions.dto.CohortSwapApprovalRequest request = com.hospital.admissions.dto.CohortSwapApprovalRequest.builder()
                .admissionRequestId(reqId)
                .targetBedId(targetBedId)
                .build();

        assertThatThrownBy(() -> bmuService.approveCohortSwap(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("departed ED");
    }

    @Test
    @DisplayName("approveCohortSwap: Rejects with 409 Conflict if target bed is not EMPTY_CLEANED")
    void testApproveCohortSwap_Conflict_WhenTargetBedNotClean() {
        UUID reqId = UUID.randomUUID();
        UUID targetBedId = UUID.randomUUID();
        Patient patient = Patient.builder().id(UUID.randomUUID()).name("John Doe").build();
        Bed flexBed = Bed.builder().id(UUID.randomUUID()).bedNumber("9A-01").status(BedStatus.EMPTY_ASSIGNED).currentPatient(patient).build();
        Bed targetBed = Bed.builder().id(targetBedId).bedNumber("8A-03").status(BedStatus.EMPTY_ASSIGNED).build(); // Target bed already claimed

        AdmissionRequest req = AdmissionRequest.builder()
                .id(reqId)
                .patient(patient)
                .assignedBed(flexBed)
                .waitingInEd(true)
                .build();

        when(admissionRequestRepository.findById(reqId)).thenReturn(Optional.of(req));
        when(bedRepository.findById(targetBedId)).thenReturn(Optional.of(targetBed));

        com.hospital.admissions.dto.CohortSwapApprovalRequest request = com.hospital.admissions.dto.CohortSwapApprovalRequest.builder()
                .admissionRequestId(reqId)
                .targetBedId(targetBedId)
                .build();

        assertThatThrownBy(() -> bmuService.approveCohortSwap(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no longer available");
    }

    @Test
    @DisplayName("referToSisterHospital: For MIC@Home allocates virtual bed and transitions status to DIVERTED_HAH")
    void testReferToSisterHospital_MicAtHome_AllocatesVirtualBedAndSetsStatus() {
        UUID reqId = UUID.randomUUID();
        Patient patient = Patient.builder().id(UUID.randomUUID()).name("Virtual Patient").build();
        AdmissionRequest req = AdmissionRequest.builder()
                .id(reqId)
                .patient(patient)
                .primaryAcuityTier(AcuityTier.TIER_4_SUBACUTE_DIVERSION)
                .status(AdmissionStatus.BED_REQUESTED)
                .build();

        SisterHospitalReferralResponse response = SisterHospitalReferralResponse.builder()
                .referralId("REF-MIC-001")
                .destinationFacility("Mobile Inpatient Care at Home (MIC@Home)")
                .status("ACCEPTED_30MIN_SLA")
                .slaWindowMinutes(30)
                .build();

        when(admissionRequestRepository.findById(reqId)).thenReturn(Optional.of(req));
        when(sisterHospitalGateway.referPatient(eq(patient), eq(req), anyString())).thenReturn(response);
        when(admissionRequestRepository.save(req)).thenReturn(req);

        SisterHospitalReferralResponse result = bmuService.referToSisterHospital(reqId, "Mobile Inpatient Care at Home (MIC@Home)");

        assertThat(result.getReferralId()).isEqualTo("REF-MIC-001");
        assertThat(req.getStatus()).isEqualTo(AdmissionStatus.DIVERTED_HAH);
        assertThat(req.getVirtualBedNumber()).isNotNull();
        assertThat(req.getVirtualBedNumber()).startsWith("MIC-V");
        assertThat(req.getReferralDispatchedAt()).isNotNull();
        assertThat(req.getReferralSlaMinutes()).isEqualTo(30);
    }

    @Test
    @DisplayName("recallDiversionToAcuteQueue: Cancels referral, returns to acute BED_REQUESTED, and logs audit")
    void testRecallDiversionToAcuteQueue_Success() {
        UUID reqId = UUID.randomUUID();
        AdmissionRequest req = AdmissionRequest.builder()
                .id(reqId)
                .status(AdmissionStatus.BED_REQUESTED)
                .diversionRecommended(true)
                .sisterHospitalReferralId("REF-OCH-1234")
                .referralDispatchedAt(LocalDateTime.now().minusMinutes(35))
                .build();

        when(admissionRequestRepository.findById(reqId)).thenReturn(Optional.of(req));
        when(admissionRequestRepository.save(req)).thenReturn(req);

        AdmissionRequest recalled = bmuService.recallDiversionToAcuteQueue(reqId);

        assertThat(recalled.getStatus()).isEqualTo(AdmissionStatus.BED_REQUESTED);
        assertThat(recalled.getSisterHospitalReferralId()).isNull();
        assertThat(recalled.isDiversionRecommended()).isFalse();
        assertThat(recalled.getReferralDispatchedAt()).isNull();
        verify(auditLogger).logAction(eq("bmu_coord"), eq("RECALL_TO_ACUTE_QUEUE"), eq("AdmissionRequest:" + reqId), anyString());
    }

    @Test
    @DisplayName("extendDiversionSla: Extends deadline by +15 mins and sets operational delay reason")
    void testExtendDiversionSla_Success() {
        UUID reqId = UUID.randomUUID();
        AdmissionRequest req = AdmissionRequest.builder()
                .id(reqId)
                .referralSlaMinutes(30)
                .referralDispatchedAt(LocalDateTime.now().minusMinutes(31))
                .build();

        when(admissionRequestRepository.findById(reqId)).thenReturn(Optional.of(req));
        when(admissionRequestRepository.save(req)).thenReturn(req);

        AdmissionRequest extended = bmuService.extendDiversionSla(reqId);

        assertThat(extended.getReferralSlaMinutes()).isEqualTo(45);
        assertThat(extended.getOperationalDelayReason()).contains("15");
        verify(auditLogger).logAction(eq("bmu_coord"), eq("EXTEND_DIVERSION_SLA"), eq("AdmissionRequest:" + reqId), anyString());
    }

    @Test
    @DisplayName("logTelephoneFollowUp: Records communication note and logs audit event")
    void testLogTelephoneFollowUp_Success() {
        UUID reqId = UUID.randomUUID();
        AdmissionRequest req = AdmissionRequest.builder()
                .id(reqId)
                .sisterHospitalReferralId("REF-OCH-1234")
                .build();

        when(admissionRequestRepository.findById(reqId)).thenReturn(Optional.of(req));
        when(admissionRequestRepository.save(req)).thenReturn(req);

        AdmissionRequest result = bmuService.logTelephoneFollowUp(reqId, "Confirmed bed 4B available at OCH with nurse supervisor");

        verify(auditLogger).logAction(eq("bmu_coord"), eq("LOG_TELEPHONE_FOLLOW_UP"), eq("AdmissionRequest:" + reqId), contains("Confirmed bed 4B"));
    }

    @Test
    @DisplayName("attachDelayTag: Successfully attaches delay reason tag, note, and logs TAG_DELAY_REASON audit event")
    void testAttachDelayTag_Success() {
        UUID reqId = UUID.randomUUID();
        AdmissionRequest req = AdmissionRequest.builder()
                .id(reqId)
                .primaryAcuityTier(AcuityTier.TIER_2_ACUTE_URGENT)
                .requestedAt(LocalDateTime.now().minusMinutes(75))
                .build();

        when(admissionRequestRepository.findById(reqId)).thenReturn(Optional.of(req));
        when(admissionRequestRepository.save(req)).thenReturn(req);

        DelayTagRequest delayReq = DelayTagRequest.builder()
                .delayReasonCode(DelayReasonCode.HOUSEKEEPING_DELAY)
                .note("EVS terminal cleaning underway for isolated bed")
                .build();

        AdmissionRequest result = bmuService.attachDelayTag(reqId, delayReq);

        assertThat(result.getDelayReasonTag()).isEqualTo("HOUSEKEEPING_DELAY");
        assertThat(result.getOperationalDelayReason()).isEqualTo("EVS terminal cleaning underway for isolated bed");
        verify(admissionRequestRepository).save(req);
        verify(auditLogger).logAction(eq("bmu_coord"), eq("TAG_DELAY_REASON"), eq("AdmissionRequest:" + reqId), contains("HOUSEKEEPING_DELAY"));
    }

    @Test
    @DisplayName("attachDelayTag: Rejects non-BMU coordinators with 403 AccessDeniedException")
    void testAttachDelayTag_Forbidden_WhenNonBmu() {
        UUID reqId = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("dr_tan", null, List.of(new SimpleGrantedAuthority("ROLE_ED_ATTENDING")))
        );

        DelayTagRequest delayReq = DelayTagRequest.builder()
                .delayReasonCode(DelayReasonCode.BED_SHORTAGE)
                .note("Ward full")
                .build();

        assertThatThrownBy(() -> bmuService.attachDelayTag(reqId, delayReq))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                .hasMessageContaining("Only BMU coordinators");
    }

    @Test
    @DisplayName("allocateBed: Automatically archives active delay tags into audit history")
    void testAllocateBed_ArchivesActiveDelayTag() {
        UUID reqId = UUID.randomUUID();
        UUID bedId = UUID.randomUUID();
        Ward ward = Ward.builder().id(UUID.randomUUID()).name("Ward 8A").capacity(4).wardClass(WardClass.B2).build();
        Bed bed = Bed.builder().id(bedId).bedNumber("8A-01").ward(ward).status(BedStatus.EMPTY_CLEANED).build();
        Patient patient = Patient.builder().id(UUID.randomUUID()).name("Patient").gender(Gender.MALE).build();
        AdmissionRequest req = AdmissionRequest.builder()
                .id(reqId)
                .patient(patient)
                .status(AdmissionStatus.BED_REQUESTED)
                .requestedWardClass(WardClass.B2)
                .delayReasonTag("BED_SHORTAGE")
                .operationalDelayReason("Awaiting discharge in 8A")
                .build();

        when(admissionRequestRepository.findById(reqId)).thenReturn(Optional.of(req));
        when(bedRepository.findById(bedId)).thenReturn(Optional.of(bed));
        when(admissionRequestRepository.save(req)).thenReturn(req);
        when(bedRepository.save(bed)).thenReturn(bed);

        AdmissionRequest allocated = bmuService.allocateBed(reqId, bedId);

        assertThat(allocated.getStatus()).isEqualTo(AdmissionStatus.BED_ALLOCATED);
        assertThat(allocated.getDelayReasonTag()).isNull();
        assertThat(allocated.getArchivedDelayReasonTag()).isEqualTo("BED_SHORTAGE");
        verify(auditLogger).logAction(eq("bmu_coord"), eq("ARCHIVE_DELAY_TAG"), eq("AdmissionRequest:" + reqId), contains("BED_SHORTAGE"));
    }

    @Test
    @DisplayName("getQueue: Specialist consult amendments elevate queue rank by acuity and telemetry")
    void testGetQueue_ResortsWhenAcuityOrTelemetryElevated() {
        LocalDateTime now = LocalDateTime.now();
        AdmissionRequest req1 = AdmissionRequest.builder()
                .id(UUID.randomUUID())
                .primaryAcuityTier(AcuityTier.TIER_3_ACUTE_STABLE)
                .requestedAt(now.minusMinutes(30))
                .status(AdmissionStatus.BED_REQUESTED)
                .build();

        AdmissionRequest req2 = AdmissionRequest.builder()
                .id(UUID.randomUUID())
                .primaryAcuityTier(AcuityTier.TIER_3_ACUTE_STABLE)
                .effectiveAcuityTier(AcuityTier.TIER_2_ACUTE_URGENT) // Elevated by specialist consult!
                .requestedAt(now.minusMinutes(10))
                .status(AdmissionStatus.BED_REQUESTED)
                .build();

        when(admissionRequestRepository.findByStatus(AdmissionStatus.BED_REQUESTED)).thenReturn(List.of(req1, req2));

        List<AdmissionRequest> queue = bmuService.getQueue();

        assertThat(queue).containsExactly(req2, req1); // req2 elevated ahead of req1 despite later requestedAt
    }
}
