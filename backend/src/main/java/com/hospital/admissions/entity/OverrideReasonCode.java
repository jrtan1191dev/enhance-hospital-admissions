package com.hospital.admissions.entity;

/**
 * Approved operational and clinical reason codes required when a BMU operator
 * overrides solver bed recommendations.
 */
public enum OverrideReasonCode {
    /** Attending consultant explicitly requested a specific ward or service. */
    ATTENDING_CLINICAL_REQUEST,
    /** Staffing shortage prevents occupancy in the recommended ward. */
    WARD_STAFFING_LIMITATION,
    /** Upgraded ward class provided under government subsidy protection. */
    GOVERNMENT_SUBSIDY_CLASS_UPGRADE,
    /** Portable telemetry monitor deployed to accommodate patient in non-telemetry ward. */
    EMERGENCY_PORTABLE_TELEMETRY_DEPLOYED,
    /** Proximity request from patient's family accepted by hospital administration. */
    FAMILY_PROXIMITY_REQUEST
}
