package com.hospital.admissions.dto;

import com.hospital.admissions.entity.Gender;
import com.hospital.admissions.entity.InfectionStatus;
import com.hospital.admissions.entity.WardClass;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchSuggestion {
    private String suggestionId;
    private UUID targetWardId;
    private String targetWardName;
    private List<UUID> patientIds;
    private List<String> patientNames;
    private List<UUID> admissionRequestIds;
    private WardClass commonWardClass;
    private Gender commonGender;
    private InfectionStatus commonInfectionStatus;
    private int unlockedCapacityCount;
}
