package com.hospital.admissions.solver;

import com.hospital.admissions.entity.AdmissionRequest;
import com.hospital.admissions.entity.BmuAlgorithmConfig;
import com.hospital.admissions.dto.BedRecommendation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@Profile("!prototype")
public class TimefoldBedAllocationSolver implements BedAllocationSolver {

    @Override
    public List<BedRecommendation> recommendBeds(AdmissionRequest request, BmuAlgorithmConfig config) {
        log.info("[PRODUCTION TIMEFOLD SOLVER] Triggering distributed multi-cluster constraint optimization");
        // Production Timefold / OptaPlanner engine invocation
        throw new UnsupportedOperationException("Production TimefoldBedAllocationSolver requires active Timefold solver configuration");
    }
}
