package com.hospital.admissions.dto;

import com.hospital.admissions.entity.SpecialtyCluster;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChainConsultRequest {
    @NotNull
    private SpecialtyCluster targetCluster;
    private String rationale;
}
