package com.hospital.admissions.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

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
}
