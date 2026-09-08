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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BmuService {

    private final AdmissionRequestRepository admissionRequestRepository;
    private final BedRepository bedRepository;
    private final WardRepository wardRepository;
    private final BmuAlgorithmConfigRepository configRepository;
    private final BedAllocationSolver solver;
    private final SisterHospitalGateway sisterHospitalGateway;
    private final AuditLogger auditLogger;

    public List<AdmissionRequest> getPrioritizedQueue() {
        List<AdmissionRequest> pendingRequests = admissionRequestRepository.findByStatus(AdmissionStatus.BED_REQUESTED);

        // Sort by Primary: Acuity Tier (ordinal 0=Tier1 is highest priority), Secondary: requestedAt FIFO
        return pendingRequests.stream()
                .sorted(Comparator.comparing((AdmissionRequest r) -> r.getPrimaryAcuityTier().ordinal())
                        .thenComparing(AdmissionRequest::getRequestedAt))
                .collect(Collectors.toList());
    }

    public List<BedRecommendation> getRecommendations(UUID admissionRequestId) {
        AdmissionRequest request = admissionRequestRepository.findById(admissionRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Admission request not found: " + admissionRequestId));

        BmuAlgorithmConfig config = getOrCreateConfig();
        return solver.recommendBeds(request, config);
    }

    @Transactional
    public AdmissionRequest allocateBed(UUID admissionRequestId, UUID bedId) {
        return allocateBed(admissionRequestId, bedId, 1, 85.0, null);
    }

    @Transactional
    public AdmissionRequest allocateBed(UUID admissionRequestId, UUID bedId, Integer rank, Double score, String overrideReason) {
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();

        AdmissionRequest request = admissionRequestRepository.findById(admissionRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Admission request not found: " + admissionRequestId));

        Bed bed = bedRepository.findById(bedId)
                .orElseThrow(() -> new IllegalArgumentException("Bed not found: " + bedId));

        if (bed.getStatus() != BedStatus.EMPTY_CLEANED) {
            throw new IllegalStateException("Bed " + bed.getBedNumber() + " is not EMPTY_CLEANED. Current status: " + bed.getStatus());
        }

        bed.setStatus(BedStatus.EMPTY_ASSIGNED);
        bed.setCurrentPatient(request.getPatient());
        bedRepository.save(bed);

        request.setStatus(AdmissionStatus.BED_ALLOCATED);
        request.setAssignedBed(bed);
        request.setAllocatedAt(LocalDateTime.now());

        boolean isOverride = (overrideReason != null && !overrideReason.isBlank());
        request.setIsRecommendationAccepted(!isOverride);
        if (isOverride) {
            request.setOverrideReasonCode(overrideReason);
        }

        AdmissionRequest savedRequest = admissionRequestRepository.save(request);

        java.util.Map<String, Object> details = new java.util.LinkedHashMap<>();
        details.put("AssignedBed", bed.getBedNumber());
        details.put("Ward", bed.getWard().getName());

        if (isOverride) {
            details.put("SelectedRank", rank != null ? rank : 2);
            details.put("OverrideReason", overrideReason);
            details.put("Override", true);
            auditLogger.logAction(currentUser, "OVERRIDE_ALLOCATION",
                    "AdmissionRequest:" + admissionRequestId,
                    AuditLogger.formatDetails(details));
        } else {
            details.put("Rank", rank != null ? rank : 1);
            details.put("Score", score != null ? score : 85.0);
            details.put("Override", false);
            auditLogger.logAction(currentUser, "ALLOCATE_BED",
                    "AdmissionRequest:" + admissionRequestId,
                    AuditLogger.formatDetails(details));
        }

        return savedRequest;
    }

    public List<Ward> getInventory() {
        return wardRepository.findAllByOrderByLevelAscNameAsc();
    }

    public BmuAlgorithmConfig getOrCreateConfig() {
        return configRepository.findAll().stream().findFirst().orElseGet(() -> {
            BmuAlgorithmConfig defaultConfig = BmuAlgorithmConfig.builder()
                    .weightSpecialtyCluster(40)
                    .weightConsolidation(30)
                    .weightFallRiskStation(15)
                    .batchHoldingWardThreshold(3)
                    .build();
            return configRepository.save(defaultConfig);
        });
    }

    @Transactional
    public BmuAlgorithmConfig updateConfig(BmuConfigUpdateRequest req) {
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
        BmuAlgorithmConfig config = getOrCreateConfig();
        config.setWeightSpecialtyCluster(req.getWeightSpecialtyCluster());
        config.setWeightConsolidation(req.getWeightConsolidation());
        config.setWeightFallRiskStation(req.getWeightFallRiskStation());
        config.setBatchHoldingWardThreshold(req.getBatchHoldingWardThreshold());

        BmuAlgorithmConfig updated = configRepository.save(config);
        auditLogger.logAction(currentUser, "UPDATE_BMU_CONFIG",
                "BmuAlgorithmConfig:" + updated.getId(),
                "Weights: Specialty=" + req.getWeightSpecialtyCluster() + ", Consolidation=" + req.getWeightConsolidation());
        return updated;
    }

    @Transactional
    public SisterHospitalReferralResponse referToSisterHospital(UUID admissionRequestId, String targetFacility) {
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();

        AdmissionRequest request = admissionRequestRepository.findById(admissionRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Admission request not found: " + admissionRequestId));

        SisterHospitalReferralResponse response = sisterHospitalGateway.referPatient(request.getPatient(), request, targetFacility);

        request.setDiversionRecommended(true);
        request.setSisterHospitalReferralId(response.getReferralId());
        admissionRequestRepository.save(request);

        java.util.Map<String, Object> details = new java.util.LinkedHashMap<>();
        details.put("Facility", response.getDestinationFacility());
        details.put("ReferralId", response.getReferralId());
        details.put("SlaWindowMins", 30);
        details.put("AcuityTier", request.getPrimaryAcuityTier());

        auditLogger.logAction(currentUser, "DIVERSION_REFERRAL",
                "AdmissionRequest:" + admissionRequestId,
                AuditLogger.formatDetails(details));

        return response;
    }
}
