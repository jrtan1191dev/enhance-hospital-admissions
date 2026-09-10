package com.hospital.admissions.entity;

/**
 * Dynamic operational stage representing patient progress along the discharge runway.
 */
public enum DischargeRunwayStage {
    RUNWAY_D3,
    RUNWAY_D2,
    RUNWAY_D1,
    READY_FOR_MORNING_SIGNOFF,
    MEDICATIONS_PENDING,
    READY_TO_VACATE,
    VACATED
}
