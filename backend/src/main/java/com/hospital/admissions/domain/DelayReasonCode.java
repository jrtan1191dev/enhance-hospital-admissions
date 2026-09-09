package com.hospital.admissions.domain;

public enum DelayReasonCode {
    HOUSEKEEPING_DELAY("Terminal sanitization in progress. Ward bed identified and undergoing infection-control turnover."),
    BED_SHORTAGE("High inpatient census. BMU actively reviewing ward discharges and prioritizing acute placement."),
    SPECIALIZED_ISOLATION_CLEANING("Specialized negative pressure isolation disinfection underway prior to patient transfer."),
    SURGE_TRAUMA_EVENT("Emergency department managing acute trauma surge; inpatient teams mobilizing additional flex capacity.");

    private final String familyTalkingPoints;

    DelayReasonCode(String familyTalkingPoints) {
        this.familyTalkingPoints = familyTalkingPoints;
    }

    public String getFamilyTalkingPoints() {
        return familyTalkingPoints;
    }
}
