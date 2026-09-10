package com.hospital.admissions.dto;

import com.hospital.admissions.entity.SpecialtyCluster;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Aggregate 24 to 72-hour discharge capacity projections for BMU coordinators.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BmuCapacityForecastDto {

    private int totalNext24Hours;
    private int totalNext48Hours;
    private int totalNext72Hours;
    private List<WardCapacityProjection> byWard;
    private List<ClusterCapacityProjection> byCluster;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WardCapacityProjection {
        private String wardCode;
        private SpecialtyCluster cluster;
        private int next24Hours;
        private int next48Hours;
        private int next72Hours;
        private int total;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClusterCapacityProjection {
        private SpecialtyCluster cluster;
        private int next24Hours;
        private int next48Hours;
        private int next72Hours;
        private int total;
    }
}
