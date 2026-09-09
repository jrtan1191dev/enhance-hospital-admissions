package com.hospital.admissions.service;

import com.hospital.admissions.domain.*;
import com.hospital.admissions.dto.BedRecommendation;
import com.hospital.admissions.dto.BmuConfigUpdateRequest;
import com.hospital.admissions.dto.SisterHospitalReferralResponse;
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
                new UsernamePasswordAuthenticationToken("bmu_coord", null, List.of())
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

        List<Ward> inventory = bmuService.getInventory();

        assertThat(inventory).containsExactly(ward);
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
}
