package com.hospital.admissions.solver;

import com.hospital.admissions.domain.*;
import com.hospital.admissions.dto.BedRecommendation;
import com.hospital.admissions.repository.BedRepository;
import com.hospital.admissions.repository.WardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@Profile("prototype")
@RequiredArgsConstructor
public class HeuristicBedAllocationSolver implements BedAllocationSolver {

    private final BedRepository bedRepository;
    private final WardRepository wardRepository;

    @Override
    public List<BedRecommendation> recommendBeds(AdmissionRequest request, BmuAlgorithmConfig config) {
        Patient patient = request.getPatient();
        List<Bed> allCleanBeds = bedRepository.findByStatus(BedStatus.EMPTY_CLEANED);
        List<BedRecommendation> candidates = new ArrayList<>();

        for (Bed bed : allCleanBeds) {
            Ward ward = bed.getWard();

            // Hard Constraint 1: Ward Class Match
            if (!ward.getWardClass().equals(request.getRequestedWardClass())) {
                continue;
            }

            // Hard Constraint 2: Locked Gender Match
            if (ward.getLockedGender() != null && !ward.getLockedGender().equals(patient.getGender())) {
                continue;
            }

            // Hard Constraint 3: Telemetry Requirement
            boolean requiresTelemetry = Boolean.TRUE.equals(request.getEffectiveTelemetry())
                    || Boolean.TRUE.equals(request.getPrimaryTelemetry())
                    || (patient != null && patient.isNeedsTelemetry());
            if (requiresTelemetry && !bed.isHasTelemetry()) {
                continue;
            }

            // Hard Constraint 4: Infection Control Isolation & Negative Pressure
            if (patient != null && patient.getInfectionStatus() == InfectionStatus.RESPIRATORY && !ward.isNegativePressure()) {
                continue;
            }

            List<Bed> wardBeds = bedRepository.findByWard_Id(ward.getId());
            if (patient != null && patient.getInfectionStatus() != InfectionStatus.NON_INFECTIOUS && ward.getCapacity() > 1) {
                // In multi-bed wards, only allow if ward is currently completely empty to cohort, or isolated
                long occupiedCount = wardBeds.stream()
                        .filter(b -> b.getStatus() == BedStatus.OCCUPIED_TAKEN || b.getStatus() == BedStatus.EMPTY_ASSIGNED)
                        .count();
                if (occupiedCount > 0) {
                    continue;
                }
            }

            // Soft Scoring
            int score = 0;
            List<String> breakdown = new ArrayList<>();

            // Rule 1: Specialty Cluster Alignment
            int specialtyWeight = config != null ? config.getWeightSpecialtyCluster() : 40;
            SpecialtyCluster targetCluster = request.getAdmittingSpecialtyCluster() != null
                    ? request.getAdmittingSpecialtyCluster()
                    : request.getSuspectedDiagnosisService();
            if (targetCluster != null && targetCluster.equals(ward.getServiceCluster())) {
                score += specialtyWeight;
                breakdown.add("+" + specialtyWeight + " Specialty (" + ward.getServiceCluster() + ")");
            }

            // Rule 2: Consolidation Packing Bonus
            int consolidationWeight = config != null ? config.getWeightConsolidation() : 30;
            long partiallyOccupiedCount = wardBeds.stream()
                    .filter(b -> b.getStatus() == BedStatus.OCCUPIED_TAKEN || b.getStatus() == BedStatus.EMPTY_ASSIGNED)
                    .count();
            if (partiallyOccupiedCount > 0) {
                score += consolidationWeight;
                breakdown.add("+" + consolidationWeight + " Consolidation (Preserves Flex Wards)");
            }

            // Rule 3: High Fall-Risk Proximity Bonus
            int fallRiskWeight = config != null ? config.getWeightFallRiskStation() : 15;
            if (patient.getFallRiskScore() >= 45 && bed.isNearNursingStation()) {
                score += fallRiskWeight;
                breakdown.add("+" + fallRiskWeight + " Proximity (Fall Risk " + patient.getFallRiskScore() + " >= 45)");
            }

            candidates.add(BedRecommendation.builder()
                    .bedId(bed.getId())
                    .bedNumber(bed.getBedNumber())
                    .level(ward.getLevel())
                    .wardName(ward.getName())
                    .score(score)
                    .scoreBreakdown(breakdown)
                    .isRecommended(false)
                    .build());
        }

        // Sort descending by score
        candidates.sort(Comparator.comparingInt(BedRecommendation::getScore).reversed());

        // Mark top 3 as recommended
        List<BedRecommendation> topRecommendations = candidates.stream().limit(3).collect(Collectors.toList());
        for (BedRecommendation rec : topRecommendations) {
            rec.setRecommended(true);
        }

        return topRecommendations;
    }
}
