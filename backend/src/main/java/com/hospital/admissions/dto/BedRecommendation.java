package com.hospital.admissions.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Solver recommendation representing an evaluated bed candidate with objective scores,
 * scoring breakdown components, and safety constraint evaluation results.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BedRecommendation {
    private UUID bedId;
    private String bedNumber;
    private int level;
    private String wardName;
    private int score;
    private List<String> scoreBreakdown;
    private boolean isRecommended;
    private boolean isSafetyViolated;
    private String safetyViolationReason;
    private boolean isOperationalOverride;
    private String operationalOverrideReason;

    /**
     * Constructs a bed recommendation with score breakdown and recommendation flag.
     *
     * @param bedId          unique bed identifier
     * @param bedNumber      bed number
     * @param level          ward floor level
     * @param wardName       ward name
     * @param score          computed affinity score
     * @param scoreBreakdown itemized score explanations
     * @param isRecommended  whether this recommendation is the solver's primary choice
     */
    public BedRecommendation(UUID bedId, String bedNumber, int level, String wardName, int score, List<String> scoreBreakdown, boolean isRecommended) {
        this.bedId = bedId;
        this.bedNumber = bedNumber;
        this.level = level;
        this.wardName = wardName;
        this.score = score;
        this.scoreBreakdown = scoreBreakdown;
        this.isRecommended = isRecommended;
    }
}
