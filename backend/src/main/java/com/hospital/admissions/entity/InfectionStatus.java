package com.hospital.admissions.entity;

/**
 * Infection status classifications dictating isolation and negative pressure requirements.
 */
public enum InfectionStatus {
    /** Non-infectious patient; eligible for standard cohort wards. */
    NON_INFECTIOUS,
    /** Airborne or droplet respiratory infection requiring negative pressure isolation. */
    RESPIRATORY,
    /** Methicillin-resistant Staphylococcus aureus requiring contact isolation. */
    MRSA
}
