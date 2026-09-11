package com.hospital.admissions.entity;

/**
 * Service level agreement status for 30-minute terminal bed sanitization turnover.
 */
public enum TurnoverSlaStatus {
    /** Bed turnover is currently progressing well within the 30-minute target. */
    ON_TRACK,
    /** Turnover is nearing the 30-minute threshold and requires prompt completion. */
    APPROACHING_SLA,
    /** Bed turnover elapsed time has exceeded 30 minutes without completion. */
    BREACHED
}
