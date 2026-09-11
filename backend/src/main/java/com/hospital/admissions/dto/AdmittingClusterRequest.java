package com.hospital.admissions.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.hospital.admissions.entity.SpecialtyCluster;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for updating the admitting specialty cluster of an admission request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdmittingClusterRequest {

    @NotNull
    @JsonAlias("cluster")
    private SpecialtyCluster admittingSpecialtyCluster;
}
