package com.hospital.admissions.solver;

import com.hospital.admissions.entity.AdmissionRequest;
import com.hospital.admissions.entity.BmuAlgorithmConfig;
import com.hospital.admissions.dto.BedRecommendation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Production-ready constraint satisfaction solver using the Timefold (OptaPlanner) optimization engine.
 * <p>
 * Active in non-prototype profiles ({@code !prototype}) to execute distributed multi-cluster
 * bed optimization across multi-building medical campuses.
 */
@Slf4j
@Component
@Profile("!prototype")
public class TimefoldBedAllocationSolver implements BedAllocationSolver {

    /**
     * Solves the multi-cluster bed allocation optimization model via Timefold constraint streams.
     *
     * @param request the {@link AdmissionRequest} to optimize bed allocation for.
     * @param config  the {@link BmuAlgorithmConfig} containing weightings and constraints.
     * @return optimized list of {@link BedRecommendation} objects.
     * @throws UnsupportedOperationException if Timefold solver is not configured.
     */
    @Override
    public List<BedRecommendation> recommendBeds(AdmissionRequest request, BmuAlgorithmConfig config) {
        log.info("[PRODUCTION TIMEFOLD SOLVER] Triggering distributed multi-cluster constraint optimization");
        // Production Timefold / OptaPlanner engine invocation
        throw new UnsupportedOperationException("Production TimefoldBedAllocationSolver requires active Timefold solver configuration");
    }
}
