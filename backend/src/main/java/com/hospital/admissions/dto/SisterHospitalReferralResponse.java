package com.hospital.admissions.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response payload confirming receipt and dispatch of an acute referral to a sister hospital.
 */
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
