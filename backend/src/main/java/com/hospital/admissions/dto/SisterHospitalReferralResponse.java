package com.hospital.admissions.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SisterHospitalReferralResponse {
    private String referralId;
    private String destinationFacility;
    private String status;
    private int slaWindowMinutes;
    private String notes;
}
