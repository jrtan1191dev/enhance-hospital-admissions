package com.hospital.admissions.domain;

/**
 * Service level agreement status for 30-minute terminal bed sanitization turnover.
 */
public enum TurnoverSlaStatus {
    ON_TRACK,
    APPROACHING_SLA,
    BREACHED
}
