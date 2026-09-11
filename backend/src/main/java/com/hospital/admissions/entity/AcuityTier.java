package com.hospital.admissions.entity;

/**
 * Clinical acuity triage tiers dictating patient queue priority and escalation urgency.
 */
public enum AcuityTier {
    /** Tier 1: Resuscitation / life-threatening conditions requiring immediate bed placement. */
    TIER_1_CRITICAL,
    /** Tier 2: Acute emergency requiring urgent inpatient placement within 15 minutes. */
    TIER_2_ACUTE_URGENT,
    /** Tier 3: Inpatient ward admission requiring bed within standard acute turnaround. */
    TIER_3_ACUTE_STABLE,
    /** Tier 4: Stable subacute condition eligible for community hospital or Hospital-at-Home diversion. */
    TIER_4_SUBACUTE_DIVERSION,
    /** Tier 5: Short-stay observation or ambulatory review under 24 hours. */
    TIER_5_SHORT_STAY
}
