package com.hospital.admissions.solver;

import com.hospital.admissions.entity.AdmissionRequest;
import com.hospital.admissions.entity.BmuAlgorithmConfig;
import com.hospital.admissions.dto.BedRecommendation;

import java.util.List;

public interface BedAllocationSolver {
    List<BedRecommendation> recommendBeds(AdmissionRequest request, BmuAlgorithmConfig config);
}
