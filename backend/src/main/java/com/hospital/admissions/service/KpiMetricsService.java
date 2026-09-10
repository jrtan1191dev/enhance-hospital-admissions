package com.hospital.admissions.service;

import com.hospital.admissions.dto.HospitalKpiSummaryDto;
import com.hospital.admissions.entity.*;
import com.hospital.admissions.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service calculating hospital-wide operational Key Performance Indicators (KPIs)
 * across Epics 1 through 4 with division-by-zero guards.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KpiMetricsService {

    private final AdmissionRequestRepository admissionRequestRepository;
    private final AssessmentBroadcastRepository broadcastRepository;
    private final BedRepository bedRepository;
    private final PatientAuditInteractionRepository patientAuditInteractionRepository;

    /**
     * Compute the consolidated hospital operational KPI summary.
     *
     * @param startDate optional ISO-8601 start boundary
     * @param endDate   optional ISO-8601 end boundary
     * @return populated HospitalKpiSummaryDto
     */
    public HospitalKpiSummaryDto getKpiSummary(LocalDateTime startDate, LocalDateTime endDate) {
        String periodStartStr = startDate != null ? startDate.toString() : "ALL_TIME";
        String periodEndStr = endDate != null ? endDate.toString() : "ALL_TIME";

        List<AdmissionRequest> allRequests = admissionRequestRepository.findAll();
        List<AssessmentBroadcast> allBroadcasts = broadcastRepository.findAll();

        // Apply temporal filtering
        List<AdmissionRequest> filteredRequests = allRequests.stream()
                .filter(r -> isWithinTimeWindow(r.getRequestedAt(), startDate, endDate))
                .toList();

        List<AssessmentBroadcast> filteredBroadcasts = allBroadcasts.stream()
                .filter(b -> isWithinTimeWindow(b.getCreatedAt(), startDate, endDate))
                .toList();

        // --- EPIC 1 CALCULATIONS ---
        long digitalBedRequestCount = filteredRequests.size();

        // 1. ED Assessment Turnaround Times
        List<Double> edTurnaroundList = filteredRequests.stream()
                .map(this::extractEdTurnaroundMinutes)
                .filter(Objects::nonNull)
                .sorted()
                .toList();

        double avgEdTurnaround = calculateAverage(edTurnaroundList);
        double p95EdTurnaround = calculatePercentile(edTurnaroundList, 0.95);

        // 2. Specialist Claim Latency
        List<Double> claimLatencies = filteredBroadcasts.stream()
                .filter(b -> b.getClaimedAt() != null && b.getCreatedAt() != null)
                .map(b -> {
                    long mins = Duration.between(b.getCreatedAt(), b.getClaimedAt()).toMinutes();
                    return mins > 0 ? (double) mins : 0.5;
                })
                .toList();

        double avgSpecialistClaimLatency = calculateAverage(claimLatencies);

        // 3. Primary vs Specialist Concordance Rate
        List<AssessmentBroadcast> completedConsults = filteredBroadcasts.stream()
                .filter(b -> b.getStatus() == BroadcastStatus.COMPLETED)
                .toList();

        double concordanceRatePct = 0.0;
        if (!completedConsults.isEmpty()) {
            long concordantCount = completedConsults.stream()
                    .filter(this::isConsultConcordant)
                    .count();
            concordanceRatePct = roundToOneDecimal((concordantCount * 100.0) / completedConsults.size());
        }

        // --- EPIC 2 CALCULATIONS: BMU Capacity & Diversions ---
        List<AdmissionRequest> allocatedRequests = filteredRequests.stream()
                .filter(r -> r.getAllocatedAt() != null || r.getAssignedBed() != null || Boolean.TRUE.equals(r.getIsRecommendationAccepted()) || (r.getOverrideReasonCode() != null && !r.getOverrideReasonCode().isBlank()))
                .toList();

        long totalAllocations = allocatedRequests.size();
        long acceptedAllocations = allocatedRequests.stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsRecommendationAccepted()))
                .count();

        List<AdmissionRequest> overriddenRequests = allocatedRequests.stream()
                .filter(r -> Boolean.FALSE.equals(r.getIsRecommendationAccepted()) || (r.getOverrideReasonCode() != null && !r.getOverrideReasonCode().isBlank()))
                .toList();
        long bmuManualOverrideCount = overriddenRequests.size();

        double bmuSuggestionAcceptanceRatePct = totalAllocations > 0
                ? roundToOneDecimal((acceptedAllocations * 100.0) / totalAllocations)
                : 0.0;

        Map<String, Long> overrideReasonsBreakdown = overriddenRequests.stream()
                .filter(r -> r.getOverrideReasonCode() != null && !r.getOverrideReasonCode().isBlank())
                .collect(Collectors.groupingBy(AdmissionRequest::getOverrideReasonCode, LinkedHashMap::new, Collectors.counting()));

        // Diversions
        List<AdmissionRequest> diversionRequests = filteredRequests.stream()
                .filter(r -> r.getReferralDispatchedAt() != null || r.getSisterHospitalReferralId() != null
                        || r.getStatus() == AdmissionStatus.DIVERTED_HAH
                        || (r.getDiversionPathway() != null && r.getDiversionPathway() != DiversionPathway.NONE))
                .toList();

        long totalDiversionCount = diversionRequests.size();
        double diversionRatePct = digitalBedRequestCount > 0
                ? roundToOneDecimal((totalDiversionCount * 100.0) / digitalBedRequestCount)
                : 0.0;

        // Sister Hospital SLA (30-minute window)
        List<AdmissionRequest> dispatchedReferrals = filteredRequests.stream()
                .filter(r -> r.getReferralDispatchedAt() != null)
                .toList();

        double sisterHospitalSlaCompliancePct = 0.0;
        if (!dispatchedReferrals.isEmpty()) {
            long compliantCount = dispatchedReferrals.stream()
                    .filter(r -> {
                        long elapsed;
                        if (r.getReferralCompletedAt() != null) {
                            elapsed = Duration.between(r.getReferralDispatchedAt(), r.getReferralCompletedAt()).toMinutes();
                        } else if (r.getAllocatedAt() != null) {
                            elapsed = Duration.between(r.getReferralDispatchedAt(), r.getAllocatedAt()).toMinutes();
                        } else {
                            elapsed = 10;
                        }
                        int slaWindow = r.getReferralSlaMinutes() != null ? r.getReferralSlaMinutes() : 30;
                        return elapsed <= slaWindow;
                    })
                    .count();
            sisterHospitalSlaCompliancePct = roundToOneDecimal((compliantCount * 100.0) / dispatchedReferrals.size());
        }

        // Diversion Channels Breakdown
        Map<String, Long> diversionChannelsBreakdown = diversionRequests.stream()
                .map(r -> {
                    boolean isMic = (r.getDiversionPathway() == DiversionPathway.HOSPITAL_AT_HOME_MIC)
                            || (r.getReferralFacility() != null && (r.getReferralFacility().toUpperCase().contains("MIC") || r.getReferralFacility().toUpperCase().contains("HOME")));
                    return isMic ? "MIC_AT_HOME" : "COMMUNITY_HOSPITAL";
                })
                .collect(Collectors.groupingBy(channel -> channel, LinkedHashMap::new, Collectors.counting()));

        // Batch Holding Ward Adoption Rate
        long batchHoldingWardAllocations = allocatedRequests.stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsBatchHoldingWardAllocated())
                        || (r.getAssignedBed() != null && r.getAssignedBed().getWard() != null && r.getAssignedBed().getWard().isHoldingWard()))
                .count();

        double batchHoldingWardAdoptionRatePct = totalAllocations > 0
                ? roundToOneDecimal((batchHoldingWardAllocations * 100.0) / totalAllocations)
                : 0.0;

        // --- EPIC 3 CALCULATIONS: Patient Experience & Counseling ---
        // 1. Patient Tracker Access Rate (KPI 14)
        long trackerAccessedCount = filteredRequests.stream()
                .filter(r -> r.getFirstTrackerAccessedAt() != null)
                .count();

        double patientTrackerAccessRatePct = digitalBedRequestCount > 0
                ? roundToOneDecimal((trackerAccessedCount * 100.0) / digitalBedRequestCount)
                : 0.0;

        // 2. 2-Hour Periodic Update Delivery (KPI 15) & Prolonged-Wait Delay Tagging
        List<AdmissionRequest> longWaitingRequests = filteredRequests.stream()
                .filter(r -> {
                    LocalDateTime reqTime = r.getRequestedAt() != null ? r.getRequestedAt() : r.getCreatedAt();
                    if (reqTime == null) return false;
                    LocalDateTime endTime = r.getAllocatedAt() != null ? r.getAllocatedAt() : (endDate != null ? endDate : LocalDateTime.now());
                    long dwellMins = Math.max(0, Duration.between(reqTime, endTime).toMinutes());
                    return dwellMins >= 120 || r.getLastPeriodicUpdateSentAt() != null;
                })
                .toList();

        long longWaitingCount = longWaitingRequests.size();
        long updateDeliveredCount = longWaitingRequests.stream()
                .filter(r -> r.getLastPeriodicUpdateSentAt() != null)
                .count();

        double twoHourPeriodicUpdateDeliveryPct = longWaitingCount > 0
                ? roundToOneDecimal((updateDeliveredCount * 100.0) / longWaitingCount)
                : 0.0;

        long delayTaggedCount = longWaitingRequests.stream()
                .filter(r -> (r.getDelayReasonTag() != null && !r.getDelayReasonTag().isBlank())
                        || (r.getArchivedDelayReasonTag() != null && !r.getArchivedDelayReasonTag().isBlank()))
                .count();

        double prolongedWaitCommunicationRatePct = longWaitingCount > 0
                ? roundToOneDecimal((delayTaggedCount * 100.0) / longWaitingCount)
                : 0.0;

        // 3. Early Caregiver Counseling Connect Rate (KPI 18)
        List<AdmissionRequest> diversionCandidates = filteredRequests.stream()
                .filter(r -> r.isDiversionRecommended()
                        || (r.getDiversionPathway() != null && r.getDiversionPathway() != DiversionPathway.NONE)
                        || r.getSisterHospitalReferralId() != null
                        || r.getStatus() == AdmissionStatus.DIVERTED_HAH)
                .toList();

        List<PatientAuditInteraction> filteredInteractions = patientAuditInteractionRepository.findAll().stream()
                .filter(i -> isWithinTimeWindow(i.getCreatedAt(), startDate, endDate))
                .filter(i -> "MSW_CALL".equalsIgnoreCase(i.getActionType())
                        || "FINANCE_CALL".equalsIgnoreCase(i.getActionType())
                        || "FINANCIAL_COUNSELING".equalsIgnoreCase(i.getActionType())
                        || "CONNECT_MSW_HOTLINE".equalsIgnoreCase(i.getActionType())
                        || "CONNECT_FINANCIAL_COUNSELING".equalsIgnoreCase(i.getActionType()))
                .toList();

        Set<UUID> contactedAdmissionIds = filteredInteractions.stream()
                .map(PatientAuditInteraction::getAdmissionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<String> contactedTokens = filteredInteractions.stream()
                .map(PatientAuditInteraction::getToken)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        long candidatesConnectedCount = diversionCandidates.stream()
                .filter(r -> contactedAdmissionIds.contains(r.getId())
                        || (r.getPatient() != null && contactedTokens.contains(r.getPatient().getQueueToken())))
                .count();

        double caregiverCounselingConnectRatePct = !diversionCandidates.isEmpty()
                ? roundToOneDecimal((candidatesConnectedCount * 100.0) / diversionCandidates.size())
                : 0.0;

        // --- EPIC 4 CALCULATIONS: Inpatient Discharge Runway & Turnover ---
        List<AdmissionRequest> dischargedRequests = filteredRequests.stream()
                .filter(r -> r.getDischargedAt() != null || r.getStatus() == AdmissionStatus.DISCHARGED)
                .toList();

        long totalDischarges = dischargedRequests.size();

        // 1. Discharge Before Noon Rate (KPI 19)
        long beforeNoonCount = dischargedRequests.stream()
                .filter(r -> r.getDischargedAt() != null && r.getDischargedAt().getHour() < 12)
                .count();

        double dischargeBeforeNoonRatePct = totalDischarges > 0
                ? roundToOneDecimal((beforeNoonCount * 100.0) / totalDischarges)
                : 0.0;

        // 2. Advance Runway Establishment Rate (KPI 20)
        List<AdmissionRequest> inpatientCohort = filteredRequests.stream()
                .filter(r -> r.getAdmittedAt() != null || r.getDischargedAt() != null
                        || r.getStatus() == AdmissionStatus.ADMITTED_INPATIENT || r.getStatus() == AdmissionStatus.DISCHARGED)
                .toList();

        long advanceRunwayCount = inpatientCohort.stream()
                .filter(r -> {
                    if (r.getEdd() == null) return false;
                    LocalDateTime refDeparture = r.getDischargedAt() != null
                            ? r.getDischargedAt()
                            : r.getEdd().atTime(12, 0);
                    LocalDateTime recordedTime = r.getEddRecordedAt() != null
                            ? r.getEddRecordedAt()
                            : (r.getAdmittedAt() != null ? r.getAdmittedAt() : r.getRequestedAt());
                    if (recordedTime == null) return false;
                    return Duration.between(recordedTime, refDeparture).toHours() >= 48;
                })
                .count();

        double advanceRunwayEstablishmentRatePct = !inpatientCohort.isEmpty()
                ? roundToOneDecimal((advanceRunwayCount * 100.0) / inpatientCohort.size())
                : 0.0;

        // 3. Bedside Discharge Medication Delivery Adoption (KPI 21)
        long bedsideMedsCount = dischargedRequests.stream()
                .filter(r -> r.getMedicationDeliveryStatus() == MedicationDeliveryStatus.DELIVERED_BEDSIDE)
                .count();

        double bedsideMedicationDeliveryAdoptionPct = totalDischarges > 0
                ? roundToOneDecimal((bedsideMedsCount * 100.0) / totalDischarges)
                : 0.0;

        // 4. Housekeeping Turnover & 30m SLA Compliance (KPI 22)
        List<Bed> allBeds = bedRepository.findAll();
        List<Double> cleaningDurations = allBeds.stream()
                .filter(b -> b.getLastCleanedAt() != null && b.getCleaningStartedAt() != null)
                .filter(b -> isWithinTimeWindow(b.getLastCleanedAt(), startDate, endDate))
                .map(b -> (double) Math.max(0, Duration.between(b.getCleaningStartedAt(), b.getLastCleanedAt()).toMinutes()))
                .toList();

        double housekeepingTurnoverAvgMinutes = roundToOneDecimal(calculateAverage(cleaningDurations));
        long within30mCount = cleaningDurations.stream()
                .filter(duration -> duration <= 30.0)
                .count();

        double housekeeping30mSlaCompliancePct = !cleaningDurations.isEmpty()
                ? roundToOneDecimal((within30mCount * 100.0) / cleaningDurations.size())
                : 0.0;

        return HospitalKpiSummaryDto.builder()
                .periodStart(periodStartStr)
                .periodEnd(periodEndStr)
                .avgEdTurnaroundMinutes(roundToOneDecimal(avgEdTurnaround))
                .edTurnaroundP95Minutes(roundToOneDecimal(p95EdTurnaround))
                .specialistClaimLatencyAvgMinutes(roundToOneDecimal(avgSpecialistClaimLatency))
                .primarySpecialistConcordanceRatePct(concordanceRatePct)
                .digitalBedRequestCount(digitalBedRequestCount)
                .bmuSuggestionAcceptanceRatePct(bmuSuggestionAcceptanceRatePct)
                .bmuManualOverrideCount(bmuManualOverrideCount)
                .totalDiversionCount(totalDiversionCount)
                .diversionRatePct(diversionRatePct)
                .sisterHospitalSlaCompliancePct(sisterHospitalSlaCompliancePct)
                .batchHoldingWardAdoptionRatePct(batchHoldingWardAdoptionRatePct)
                .overrideReasonsBreakdown(overrideReasonsBreakdown)
                .diversionChannelsBreakdown(diversionChannelsBreakdown)
                .patientTrackerAccessRatePct(patientTrackerAccessRatePct)
                .twoHourPeriodicUpdateDeliveryPct(twoHourPeriodicUpdateDeliveryPct)
                .prolongedWaitCommunicationRatePct(prolongedWaitCommunicationRatePct)
                .caregiverCounselingConnectRatePct(caregiverCounselingConnectRatePct)
                .dischargeBeforeNoonRatePct(dischargeBeforeNoonRatePct)
                .advanceRunwayEstablishmentRatePct(advanceRunwayEstablishmentRatePct)
                .bedsideMedicationDeliveryAdoptionPct(bedsideMedicationDeliveryAdoptionPct)
                .housekeepingTurnoverAvgMinutes(housekeepingTurnoverAvgMinutes)
                .housekeeping30mSlaCompliancePct(housekeeping30mSlaCompliancePct)
                .build();
    }

    private boolean isWithinTimeWindow(LocalDateTime timestamp, LocalDateTime start, LocalDateTime end) {
        if (timestamp == null) {
            return start == null && end == null;
        }
        if (start != null && timestamp.isBefore(start)) {
            return false;
        }
        if (end != null && timestamp.isAfter(end)) {
            return false;
        }
        return true;
    }

    private Double extractEdTurnaroundMinutes(AdmissionRequest request) {
        if (request.getEdTurnaroundMinutes() != null) {
            return request.getEdTurnaroundMinutes();
        }
        if (request.getPatient() != null && request.getPatient().getCreatedAt() != null && request.getRequestedAt() != null) {
            return (double) Math.max(0, Duration.between(request.getPatient().getCreatedAt(), request.getRequestedAt()).toMinutes());
        }
        if (request.getRequestedAt() != null && request.getCreatedAt() != null) {
            return (double) Math.max(0, Duration.between(request.getCreatedAt(), request.getRequestedAt()).toMinutes());
        }
        return null;
    }

    private boolean isConsultConcordant(AssessmentBroadcast broadcast) {
        if (broadcast.getIsConcordant() != null) {
            return broadcast.getIsConcordant();
        }
        if (broadcast.getAdmissionRequest() != null && broadcast.getSecondaryAcuityTier() != null) {
            boolean acuityMatches = broadcast.getSecondaryAcuityTier() == broadcast.getAdmissionRequest().getPrimaryAcuityTier();
            boolean telemetryMatches = broadcast.getSecondaryTelemetry() == null
                    || broadcast.getSecondaryTelemetry().equals(broadcast.getAdmissionRequest().getPrimaryTelemetry());
            return acuityMatches && telemetryMatches;
        }
        return false;
    }

    private double calculateAverage(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }
        double sum = 0.0;
        for (Double v : values) {
            sum += v;
        }
        return sum / values.size();
    }

    private double calculatePercentile(List<Double> sortedValues, double percentile) {
        if (sortedValues == null || sortedValues.isEmpty()) {
            return 0.0;
        }
        int index = (int) Math.ceil(percentile * sortedValues.size()) - 1;
        if (index < 0) index = 0;
        if (index >= sortedValues.size()) index = sortedValues.size() - 1;
        return sortedValues.get(index);
    }

    private double roundToOneDecimal(double val) {
        return Math.round(val * 10.0) / 10.0;
    }
}
