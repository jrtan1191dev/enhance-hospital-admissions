package com.hospital.admissions.entity;

/**
 * Standard delay reason categories for patient admissions and transfers,
 * including standardized patient/family talking points.
 */
public enum DelayReasonCode {
    /** Delay caused by housekeeping and room sanitization. */
    HOUSEKEEPING_DELAY("Terminal sanitization in progress. Ward bed identified and undergoing infection-control turnover."),
    /** Delay caused by high inpatient occupancy and bed shortages. */
    BED_SHORTAGE("High inpatient census. BMU actively reviewing ward discharges and prioritizing acute placement."),
    /** Delay caused by negative pressure isolation room cleaning procedures. */
    SPECIALIZED_ISOLATION_CLEANING("Specialized negative pressure isolation disinfection underway prior to patient transfer."),
    /** Delay caused by mass casualty or acute emergency trauma surge. */
    SURGE_TRAUMA_EVENT("Emergency department managing acute trauma surge; inpatient teams mobilizing additional flex capacity.");

    private final String familyTalkingPoints;

    DelayReasonCode(String familyTalkingPoints) {
        this.familyTalkingPoints = familyTalkingPoints;
    }

    /**
     * Returns the standardized talking points for communication with patients and families.
     *
     * @return explanation message for families
     */
    public String getFamilyTalkingPoints() {
        return familyTalkingPoints;
    }
}
