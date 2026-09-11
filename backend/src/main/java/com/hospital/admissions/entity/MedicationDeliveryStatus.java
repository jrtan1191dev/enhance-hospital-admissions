package com.hospital.admissions.entity;

/**
 * Inpatient bedside discharge medication dispensing and delivery status.
 */
public enum MedicationDeliveryStatus {
    /** Medication dispensing has not yet been triggered by pharmacy. */
    NOT_DISPATCHED,
    /** Medication order is actively being packed by the automated dispensing system. */
    PACKING_IN_PROGRESS,
    /** Discharge medication package has arrived at the patient's bedside. */
    DELIVERED_BEDSIDE
}
