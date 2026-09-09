package com.hospital.admissions.dto;

import com.hospital.admissions.domain.Gender;
import com.hospital.admissions.domain.InfectionStatus;
import com.hospital.admissions.domain.SpecialtyCluster;
import com.hospital.admissions.domain.WardClass;

import java.util.List;
import java.util.UUID;

public record WardDto(
        UUID id,
        String wardCode,
        int levelNumber,
        WardClass wardClass,
        SpecialtyCluster specialty,
        Gender genderCohortLocked,
        InfectionStatus infectionLocked,
        List<BedDto> beds
) {}
