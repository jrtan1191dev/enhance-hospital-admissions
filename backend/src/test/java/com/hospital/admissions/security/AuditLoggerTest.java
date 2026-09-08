package com.hospital.admissions.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;

class AuditLoggerTest {

    private final AuditLogger auditLogger = new AuditLogger();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("logAction with specific user logs and cleans up MDC")
    void testLogAction_WithUser() {
        auditLogger.logAction("dr_smith", "APPROVE_BED", "Bed:123", "Approved successfully");

        assertThat(MDC.get("auditUser")).isNull();
        assertThat(MDC.get("auditAction")).isNull();
        assertThat(MDC.get("auditTarget")).isNull();
    }

    @Test
    @DisplayName("logAction with Map formats details key-values and cleans up MDC")
    void testLogAction_WithMap() {
        java.util.Map<String, Object> details = new java.util.LinkedHashMap<>();
        details.put("BedNumber", "8A-01");
        details.put("ElapsedCleaningMins", 22);
        details.put("Within30mSla", true);

        auditLogger.logAction("nurse_jane", "CLEAN_BED", "Bed:456", details);

        assertThat(MDC.get("auditUser")).isNull();
        assertThat(MDC.get("auditAction")).isNull();
        assertThat(MDC.get("auditTarget")).isNull();
    }

    @Test
    @DisplayName("logAction with null or empty Map handles gracefully")
    void testLogAction_EmptyOrNullMap() {
        auditLogger.logAction("nurse_jane", "TRACK_ACCESS", "PatientToken:tok123", (java.util.Map<String, Object>) null);
        auditLogger.logAction("nurse_jane", "TRACK_ACCESS", "PatientToken:tok123", java.util.Map.of());

        assertThat(MDC.get("auditUser")).isNull();
    }
}
