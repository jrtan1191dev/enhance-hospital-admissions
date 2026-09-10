package com.hospital.admissions.service;

import com.hospital.admissions.dto.HospitalKpiSummaryDto;
import com.hospital.admissions.entity.*;
import com.hospital.admissions.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KpiMetricsServiceTest {

    @Mock
    private AdmissionRequestRepository admissionRequestRepository;
    @Mock
    private AssessmentBroadcastRepository broadcastRepository;
    @Mock
    private BedRepository bedRepository;
    @Mock
    private PatientAuditInteractionRepository patientAuditInteractionRepository;

    private KpiMetricsService kpiMetricsService;

    @BeforeEach
    void setUp() {
        kpiMetricsService = new KpiMetricsService(
                admissionRequestRepository,
                broadcastRepository,
                bedRepository,
                patientAuditInteractionRepository
        );
    }

    @Test
    @DisplayName("Empty cohorts return 0.0 for all averages, percentiles, rates, and 0 for counts")
    void testEmptyCohorts_ReturnsZeroGuardedMetrics() {
        when(admissionRequestRepository.findAll()).thenReturn(Collections.emptyList());
        when(broadcastRepository.findAll()).thenReturn(Collections.emptyList());

        HospitalKpiSummaryDto summary = kpiMetricsService.getKpiSummary(null, null);

        assertThat(summary.getPeriodStart()).isEqualTo("ALL_TIME");
        assertThat(summary.getPeriodEnd()).isEqualTo("ALL_TIME");
        assertThat(summary.getAvgEdTurnaroundMinutes()).isEqualTo(0.0);
        assertThat(summary.getEdTurnaroundP95Minutes()).isEqualTo(0.0);
        assertThat(summary.getSpecialistClaimLatencyAvgMinutes()).isEqualTo(0.0);
        assertThat(summary.getPrimarySpecialistConcordanceRatePct()).isEqualTo(0.0);
        assertThat(summary.getDigitalBedRequestCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Calculates Epic 1 operational turnaround, claim latency, concordance, and counts correctly")
    void testEpic1Calculations_AccurateFormulas() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 10, 10, 0);

        // 3 admission requests
        AdmissionRequest r1 = AdmissionRequest.builder()
                .id(UUID.randomUUID())
                .requestedAt(now)
                .edTurnaroundMinutes(10.0)
                .build();
        AdmissionRequest r2 = AdmissionRequest.builder()
                .id(UUID.randomUUID())
                .requestedAt(now.plusMinutes(5))
                .edTurnaroundMinutes(20.0)
                .build();
        AdmissionRequest r3 = AdmissionRequest.builder()
                .id(UUID.randomUUID())
                .requestedAt(now.plusMinutes(10))
                .edTurnaroundMinutes(30.0)
                .build();

        when(admissionRequestRepository.findAll()).thenReturn(List.of(r1, r2, r3));

        // 2 claimed broadcasts: latency 5 mins and 15 mins -> avg 10.0 mins
        AssessmentBroadcast b1 = AssessmentBroadcast.builder()
                .id(UUID.randomUUID())
                .claimedAt(now.plusMinutes(5))
                .status(BroadcastStatus.COMPLETED)
                .isConcordant(true)
                .build();
        b1.setCreatedAt(now);
        AssessmentBroadcast b2 = AssessmentBroadcast.builder()
                .id(UUID.randomUUID())
                .claimedAt(now.plusMinutes(15))
                .status(BroadcastStatus.COMPLETED)
                .isConcordant(true)
                .build();
        b2.setCreatedAt(now);
        // 2 more completed broadcasts to test concordance: 1 concordant, 1 discordant (total: 3 concordant / 4 total = 75.0%)
        AssessmentBroadcast b3 = AssessmentBroadcast.builder()
                .id(UUID.randomUUID())
                .claimedAt(null)
                .status(BroadcastStatus.COMPLETED)
                .isConcordant(true)
                .build();
        b3.setCreatedAt(now);
        AssessmentBroadcast b4 = AssessmentBroadcast.builder()
                .id(UUID.randomUUID())
                .claimedAt(null)
                .status(BroadcastStatus.COMPLETED)
                .isConcordant(false)
                .build();
        b4.setCreatedAt(now);

        when(broadcastRepository.findAll()).thenReturn(List.of(b1, b2, b3, b4));

        HospitalKpiSummaryDto summary = kpiMetricsService.getKpiSummary(null, null);

        assertThat(summary.getDigitalBedRequestCount()).isEqualTo(3);
        assertThat(summary.getAvgEdTurnaroundMinutes()).isEqualTo(20.0);
        assertThat(summary.getEdTurnaroundP95Minutes()).isEqualTo(30.0);
        assertThat(summary.getSpecialistClaimLatencyAvgMinutes()).isEqualTo(10.0);
        assertThat(summary.getPrimarySpecialistConcordanceRatePct()).isEqualTo(75.0);
    }

    @Test
    @DisplayName("Temporal filtering includes only records falling within [startDate, endDate]")
    void testTemporalFiltering() {
        LocalDateTime start = LocalDateTime.of(2026, 9, 10, 8, 0);
        LocalDateTime end = LocalDateTime.of(2026, 9, 10, 12, 0);

        AdmissionRequest inWindow = AdmissionRequest.builder()
                .id(UUID.randomUUID())
                .requestedAt(start.plusHours(1))
                .edTurnaroundMinutes(15.0)
                .build();
        AdmissionRequest outWindow = AdmissionRequest.builder()
                .id(UUID.randomUUID())
                .requestedAt(start.minusHours(2))
                .edTurnaroundMinutes(45.0)
                .build();

        when(admissionRequestRepository.findAll()).thenReturn(List.of(inWindow, outWindow));
        when(broadcastRepository.findAll()).thenReturn(Collections.emptyList());

        HospitalKpiSummaryDto summary = kpiMetricsService.getKpiSummary(start, end);

        assertThat(summary.getPeriodStart()).isEqualTo(start.toString());
        assertThat(summary.getPeriodEnd()).isEqualTo(end.toString());
        assertThat(summary.getDigitalBedRequestCount()).isEqualTo(1);
        assertThat(summary.getAvgEdTurnaroundMinutes()).isEqualTo(15.0);
    }

    @Test
    @DisplayName("Calculates Epic 2 BMU capacity, override breakdown, diversions, and batch holding ward correctly")
    void testEpic2Calculations_AccurateFormulas() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 10, 10, 0);

        AdmissionRequest r1 = AdmissionRequest.builder()
                .id(UUID.randomUUID())
                .requestedAt(now)
                .allocatedAt(now.plusMinutes(10))
                .isRecommendationAccepted(true)
                .build();
        AdmissionRequest r2 = AdmissionRequest.builder()
                .id(UUID.randomUUID())
                .requestedAt(now)
                .allocatedAt(now.plusMinutes(12))
                .isRecommendationAccepted(true)
                .isBatchHoldingWardAllocated(true)
                .build();
        AdmissionRequest r3 = AdmissionRequest.builder()
                .id(UUID.randomUUID())
                .requestedAt(now)
                .allocatedAt(now.plusMinutes(15))
                .isRecommendationAccepted(false)
                .overrideReasonCode("GOVERNMENT_SUBSIDY_CLASS_UPGRADE")
                .build();
        AdmissionRequest r4 = AdmissionRequest.builder()
                .id(UUID.randomUUID())
                .requestedAt(now)
                .allocatedAt(now.plusMinutes(20))
                .isRecommendationAccepted(false)
                .overrideReasonCode("NON_TOP_RANK_SELECTION")
                .build();

        // 2 Diversions
        AdmissionRequest r5 = AdmissionRequest.builder()
                .id(UUID.randomUUID())
                .requestedAt(now)
                .referralDispatchedAt(now)
                .referralCompletedAt(now.plusMinutes(25))
                .referralFacility("OCH")
                .diversionPathway(DiversionPathway.COMMUNITY_HOSPITAL)
                .build();
        AdmissionRequest r6 = AdmissionRequest.builder()
                .id(UUID.randomUUID())
                .requestedAt(now)
                .referralDispatchedAt(now)
                .referralCompletedAt(now.plusMinutes(45))
                .referralFacility("MIC@Home")
                .diversionPathway(DiversionPathway.HOSPITAL_AT_HOME_MIC)
                .build();

        when(admissionRequestRepository.findAll()).thenReturn(List.of(r1, r2, r3, r4, r5, r6));
        when(broadcastRepository.findAll()).thenReturn(Collections.emptyList());

        HospitalKpiSummaryDto summary = kpiMetricsService.getKpiSummary(null, null);

        // Allocations: 4 total, 2 accepted, 2 overrides -> 50.0% acceptance
        assertThat(summary.getBmuSuggestionAcceptanceRatePct()).isEqualTo(50.0);
        assertThat(summary.getBmuManualOverrideCount()).isEqualTo(2);
        assertThat(summary.getOverrideReasonsBreakdown()).containsEntry("GOVERNMENT_SUBSIDY_CLASS_UPGRADE", 1L);
        assertThat(summary.getOverrideReasonsBreakdown()).containsEntry("NON_TOP_RANK_SELECTION", 1L);

        // Diversions: 2 out of 6 total bed requests -> 33.3%
        assertThat(summary.getTotalDiversionCount()).isEqualTo(2);
        assertThat(summary.getDiversionRatePct()).isEqualTo(33.3);
        // Transfer SLA: 1 within 30m, 1 exceeded 30m -> 50.0%
        assertThat(summary.getSisterHospitalSlaCompliancePct()).isEqualTo(50.0);
        assertThat(summary.getDiversionChannelsBreakdown()).containsEntry("COMMUNITY_HOSPITAL", 1L);
        assertThat(summary.getDiversionChannelsBreakdown()).containsEntry("MIC_AT_HOME", 1L);

        // Batch holding ward: 1 out of 4 allocations -> 25.0%
        assertThat(summary.getBatchHoldingWardAdoptionRatePct()).isEqualTo(25.0);
    }

    @Test
    @DisplayName("Calculates Epic 3 patient tracker access, periodic push, delay communication, and counseling rate correctly")
    void testEpic3Calculations_AccurateFormulas() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 10, 10, 0);

        UUID r1Id = UUID.randomUUID();
        AdmissionRequest r1 = AdmissionRequest.builder()
                .id(r1Id)
                .requestedAt(now.minusHours(3))
                .allocatedAt(now)
                .firstTrackerAccessedAt(now.minusHours(2))
                .lastPeriodicUpdateSentAt(now.minusHours(1))
                .delayReasonTag("HOUSEKEEPING_DELAY")
                .diversionRecommended(true)
                .build();

        UUID r2Id = UUID.randomUUID();
        AdmissionRequest r2 = AdmissionRequest.builder()
                .id(r2Id)
                .requestedAt(now.minusHours(3))
                .firstTrackerAccessedAt(null)
                .lastPeriodicUpdateSentAt(null)
                .delayReasonTag(null)
                .diversionRecommended(true)
                .build();

        AdmissionRequest r3 = AdmissionRequest.builder()
                .id(UUID.randomUUID())
                .requestedAt(now.minusMinutes(30))
                .allocatedAt(now.minusMinutes(10))
                .firstTrackerAccessedAt(now.minusMinutes(20))
                .diversionRecommended(false)
                .build();

        PatientAuditInteraction call1 = PatientAuditInteraction.builder()
                .id(UUID.randomUUID())
                .admissionId(r1Id)
                .token("TOK-R1")
                .actionType("MSW_CALL")
                .createdAt(now.minusHours(1))
                .build();

        when(admissionRequestRepository.findAll()).thenReturn(List.of(r1, r2, r3));
        when(broadcastRepository.findAll()).thenReturn(Collections.emptyList());
        when(patientAuditInteractionRepository.findAll()).thenReturn(List.of(call1));

        HospitalKpiSummaryDto summary = kpiMetricsService.getKpiSummary(null, null);

        // 2 out of 3 accessed tracker -> 66.7%
        assertThat(summary.getPatientTrackerAccessRatePct()).isEqualTo(66.7);
        // 2 waited >= 120 mins (r1, r2), 1 received update (r1) -> 50.0%
        assertThat(summary.getTwoHourPeriodicUpdateDeliveryPct()).isEqualTo(50.0);
        // 2 waited >= 120 mins, 1 had delay reason tag (r1) -> 50.0%
        assertThat(summary.getProlongedWaitCommunicationRatePct()).isEqualTo(50.0);
        // 2 diversion candidates (r1, r2), 1 made counseling call (r1) -> 50.0%
        assertThat(summary.getCaregiverCounselingConnectRatePct()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("Calculates Epic 4 noon discharge, 48h advance runway, bedside meds, and turnover SLA correctly")
    void testEpic4Calculations_AccurateFormulas() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 10, 10, 0);

        AdmissionRequest r1 = AdmissionRequest.builder()
                .id(UUID.randomUUID())
                .requestedAt(now.minusDays(3))
                .admittedAt(now.minusDays(2))
                .dischargedAt(now.plusDays(1).withHour(11).withMinute(30)) // discharged at 11:30 (before noon)
                .edd(java.time.LocalDate.of(2026, 9, 11))
                .eddRecordedAt(now.minusDays(2)) // 3 days before discharge (>= 48 hours)
                .medicationDeliveryStatus(MedicationDeliveryStatus.DELIVERED_BEDSIDE)
                .status(AdmissionStatus.DISCHARGED)
                .build();

        AdmissionRequest r2 = AdmissionRequest.builder()
                .id(UUID.randomUUID())
                .requestedAt(now.minusDays(2))
                .admittedAt(now.minusDays(1))
                .dischargedAt(now.withHour(14).withMinute(15)) // discharged at 14:15 (after noon)
                .edd(java.time.LocalDate.of(2026, 9, 10))
                .eddRecordedAt(now.minusHours(12)) // 12 hours before discharge (< 48 hours)
                .medicationDeliveryStatus(MedicationDeliveryStatus.NOT_DISPATCHED)
                .status(AdmissionStatus.DISCHARGED)
                .build();

        Bed b1 = Bed.builder()
                .id(UUID.randomUUID())
                .bedNumber("8A-01")
                .cleaningStartedAt(now)
                .lastCleanedAt(now.plusMinutes(20)) // 20m <= 30m SLA
                .build();

        Bed b2 = Bed.builder()
                .id(UUID.randomUUID())
                .bedNumber("8A-02")
                .cleaningStartedAt(now)
                .lastCleanedAt(now.plusMinutes(40)) // 40m > 30m SLA
                .build();

        when(admissionRequestRepository.findAll()).thenReturn(List.of(r1, r2));
        when(broadcastRepository.findAll()).thenReturn(Collections.emptyList());
        when(bedRepository.findAll()).thenReturn(List.of(b1, b2));

        HospitalKpiSummaryDto summary = kpiMetricsService.getKpiSummary(null, null);

        // Discharges: 1 out of 2 before noon -> 50.0%
        assertThat(summary.getDischargeBeforeNoonRatePct()).isEqualTo(50.0);
        // Advance Runway: 1 out of 2 recorded >= 48h prior -> 50.0%
        assertThat(summary.getAdvanceRunwayEstablishmentRatePct()).isEqualTo(50.0);
        // Bedside Meds: 1 out of 2 delivered bedside -> 50.0%
        assertThat(summary.getBedsideMedicationDeliveryAdoptionPct()).isEqualTo(50.0);
        // Housekeeping turnover avg: (20 + 40) / 2 = 30.0 mins
        assertThat(summary.getHousekeepingTurnoverAvgMinutes()).isEqualTo(30.0);
        // Housekeeping 30m SLA: 1 out of 2 <= 30 mins -> 50.0%
        assertThat(summary.getHousekeeping30mSlaCompliancePct()).isEqualTo(50.0);
    }
}
