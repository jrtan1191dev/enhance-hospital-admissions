package com.hospital.admissions.solver;

import com.hospital.admissions.entity.AdmissionRequest;
import com.hospital.admissions.entity.BmuAlgorithmConfig;
import com.hospital.admissions.dto.BedRecommendation;

import java.util.List;

/**
 * Strategy interface for scoring and recommending available inpatient beds for an admission request.
 * <p>
 * Implementations apply clinical hard constraints (ward class, infection containment, gender cohorting,
 * telemetry capabilities) and soft optimization weights (specialty service proximity, cohort consolidation,
 * fall risk nursing station proximity).
 */
public interface BedAllocationSolver {

    /**
     * Calculates ranked bed recommendations for the given admission request according to algorithm config.
     *
     * @param request the {@link AdmissionRequest} containing patient clinical requirements and triage tiers.
     * @param config  the {@link BmuAlgorithmConfig} containing optimization weights and thresholds.
     * @return list of scored and ranked {@link BedRecommendation} instances.
     */
    List<BedRecommendation> recommendBeds(AdmissionRequest request, BmuAlgorithmConfig config);
}
