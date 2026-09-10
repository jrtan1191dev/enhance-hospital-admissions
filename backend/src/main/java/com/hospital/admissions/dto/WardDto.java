package com.hospital.admissions.dto;

import com.hospital.admissions.entity.Gender;
import com.hospital.admissions.entity.InfectionStatus;
import com.hospital.admissions.entity.SpecialtyCluster;
import com.hospital.admissions.entity.WardClass;

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
