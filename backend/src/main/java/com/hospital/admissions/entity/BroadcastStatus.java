package com.hospital.admissions.entity;

/**
 * Lifecycle status of an assessment broadcast to specialists.
 */
public enum BroadcastStatus {
    /** Broadcast is published and awaiting claim by an on-call specialist. */
    OPEN,
    /** Specialist has claimed the consult and is reviewing patient data. */
    CLAIMED,
    /** Specialist response SLA elapsed; request was automatically escalated to department head. */
    AUTO_ESCALATED,
    /** Specialist assessment completed and findings recorded. */
    COMPLETED
}
