package com.hospital.admissions.entity;

/**
 * Available diversion care pathways to avoid acute inpatient admissions.
 */
public enum DiversionPathway {
    /** No diversion; acute hospital admission required. */
    NONE,
    /** Step-down transfer to a community hospital facility. */
    COMMUNITY_HOSPITAL,
    /** Mobile Inpatient Care / Hospital-at-Home program. */
    HOSPITAL_AT_HOME_MIC
}
