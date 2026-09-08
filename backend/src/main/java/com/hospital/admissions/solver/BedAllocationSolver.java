package com.hospital.admissions.solver;

import com.hospital.admissions.domain.AdmissionRequest;
import com.hospital.admissions.domain.BmuAlgorithmConfig;
import com.hospital.admissions.dto.BedRecommendation;

import java.util.List;

public interface BedAllocationSolver {
    List<BedRecommendation> recommendBeds(AdmissionRequest request, BmuAlgorithmConfig config);
}
