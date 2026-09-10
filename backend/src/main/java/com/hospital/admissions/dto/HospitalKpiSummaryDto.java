package com.hospital.admissions.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Consolidated executive summary DTO containing operational KPIs across Epics 1 through 4.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HospitalKpiSummaryDto {

    private String periodStart;
    private String periodEnd;

    // Epic 1: Clinical Intake & Collaboration
    @Builder.Default
    private double avgEdTurnaroundMinutes = 0.0;
    @Builder.Default
    private double edTurnaroundP95Minutes = 0.0;
    @Builder.Default
    private double specialistClaimLatencyAvgMinutes = 0.0;
    @Builder.Default
    private double primarySpecialistConcordanceRatePct = 0.0;
    @Builder.Default
    private long digitalBedRequestCount = 0;

    // Epic 2: BMU Capacity & Diversions
    @Builder.Default
    private double bmuSuggestionAcceptanceRatePct = 0.0;
    @Builder.Default
    private long bmuManualOverrideCount = 0;
    @Builder.Default
    private long totalDiversionCount = 0;
    @Builder.Default
    private double diversionRatePct = 0.0;
    @Builder.Default
    private double sisterHospitalSlaCompliancePct = 0.0;
    @Builder.Default
    private double batchHoldingWardAdoptionRatePct = 0.0;
    private Map<String, Long> overrideReasonsBreakdown;
    private Map<String, Long> diversionChannelsBreakdown;

    // Epic 3: Patient Experience & Counseling
    @Builder.Default
    private double patientTrackerAccessRatePct = 0.0;
    @Builder.Default
    private double twoHourPeriodicUpdateDeliveryPct = 0.0;
    @Builder.Default
    private double prolongedWaitCommunicationRatePct = 0.0;
    @Builder.Default
    private double caregiverCounselingConnectRatePct = 0.0;

    // Epic 4: Inpatient Discharge & Rapid Turnover
    @Builder.Default
    private double dischargeBeforeNoonRatePct = 0.0;
    @Builder.Default
    private double advanceRunwayEstablishmentRatePct = 0.0;
    @Builder.Default
    private double bedsideMedicationDeliveryAdoptionPct = 0.0;
    @Builder.Default
    private double housekeepingTurnoverAvgMinutes = 0.0;
    @Builder.Default
    private double housekeeping30mSlaCompliancePct = 0.0;
}
