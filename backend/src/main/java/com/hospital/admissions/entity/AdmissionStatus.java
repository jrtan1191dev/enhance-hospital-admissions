package com.hospital.admissions.entity;

/**
 * Represents the lifecycle stages of an admission request.
 */
public enum AdmissionStatus {
    /** Patient triage and clinical assessment are ongoing or awaiting specialist consult. */
    ASSESSMENT_PENDING,
    /** Clinical assessment complete; request is in BMU queue awaiting bed allocation. */
    BED_REQUESTED,
    /** BMU has assigned an inpatient bed; patient is in transit to the ward. */
    BED_ALLOCATED,
    /** Patient has arrived and checked in to the ward as an inpatient. */
    ADMITTED_INPATIENT,
    /** Patient has completed their inpatient stay and been discharged. */
    DISCHARGED,
    /** Patient was diverted to Hospital-at-Home (HaH) or community care. */
    DIVERTED_HAH
}
