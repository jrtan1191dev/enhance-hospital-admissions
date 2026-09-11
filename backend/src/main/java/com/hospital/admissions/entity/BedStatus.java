package com.hospital.admissions.entity;

import lombok.Getter;

/**
 * Status enumeration for physical hospital beds with associated UI color coding and descriptions.
 */
@Getter
public enum BedStatus {
    /**
     * MUSTARD YELLOW: Patient has been discharged, and the bed is vacated and empty, but not yet cleaned.
     */
    EMPTY_PENDING_CLEANING("MUSTARD YELLOW", "Vacated, empty, pending cleaning"),

    /**
     * WHITE: Empty, cleaned, sanitized, and immediately available for allocation.
     */
    EMPTY_CLEANED("WHITE", "Empty, cleaned"),

    /**
     * GREEN: Allocated to an admission request by BMU; patient in transit.
     */
    EMPTY_ASSIGNED("GREEN", "Allocated, in transit"),

    /**
     * GREY: Physically occupied by admitted patient in ward.
     */
    OCCUPIED_TAKEN("GREY", "Occupied");

    private final String displayColor;
    private final String description;

    BedStatus(String displayColor, String description) {
        this.displayColor = displayColor;
        this.description = description;
    }
}
