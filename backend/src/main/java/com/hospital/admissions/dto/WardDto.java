package com.hospital.admissions.dto;

import com.hospital.admissions.entity.Gender;
import com.hospital.admissions.entity.InfectionStatus;
import com.hospital.admissions.entity.SpecialtyCluster;
import com.hospital.admissions.entity.WardClass;

import java.util.List;
import java.util.UUID;

/**
 * Data transfer record representing a ward along with its beds and cohort locking status.
 *
 * @param id                  unique ward identifier
 * @param wardCode            ward name/code
 * @param levelNumber         floor level
 * @param wardClass           ward accommodation class
 * @param specialty           primary specialty cluster assigned to this ward
 * @param genderCohortLocked  gender cohort restriction currently applied to this ward
 * @param infectionLocked     infection cohort restriction currently applied to this ward
 * @param beds                list of beds within the ward
 */
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
