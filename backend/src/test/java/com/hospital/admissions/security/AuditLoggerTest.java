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
    @DisplayName("logAction with null or blank user defaults to SYSTEM")
    void testLogAction_NullUser() {
        auditLogger.logAction(null, "CLEAN_BED", "Bed:456", "Auto cleaned");
        auditLogger.logAction("   ", "CLEAN_BED", "Bed:456", "Auto cleaned");

        assertThat(MDC.get("auditUser")).isNull();
    }
}
