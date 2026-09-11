package com.hospital.admissions.security;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Structured audit logging utility providing immutable traceability for clinical and operational actions.
 * <p>
 * Enriches Logback/SLF4J MDC context with actor identity, action verb, and target entity resource IDs,
 * ensuring full audit trail compliance with healthcare security governance.
 */
@Slf4j
@Component
public class AuditLogger {

    /**
     * Emits a structured audit log entry with MDC context variables.
     *
     * @param user    the username or principal performing the operation.
     * @param action  the auditable action verb (e.g. ALLOCATE_BED, RECORD_EDD).
     * @param target  the target entity reference (e.g. AdmissionRequest:UUID).
     * @param details narrative or serialized key-value attributes describing the operation.
     */
    public void logAction(String user, String action, String target, String details) {
        String effectiveUser = (user != null && !user.isBlank()) ? user : "SYSTEM";
        MDC.put("auditUser", effectiveUser);
        MDC.put("auditAction", action);
        MDC.put("auditTarget", target);
        log.info("[AUDIT] user=\"{}\" action=\"{}\" target=\"{}\" details=\"{}\"", effectiveUser, action, target, details);
        MDC.remove("auditUser");
        MDC.remove("auditAction");
        MDC.remove("auditTarget");
    }

    /**
     * Formats a map of key-value attributes into an audit details string.
     *
     * @param detailsMap map of attribute names and values.
     * @return comma-separated representation of details.
     */
    public static String formatDetails(Map<String, ?> detailsMap) {
        if (detailsMap == null || detailsMap.isEmpty()) {
            return "";
        }
        return detailsMap.entrySet().stream()
                .map(e -> e.getKey() + "=" + (e.getValue() != null ? e.getValue().toString() : "null"))
                .collect(Collectors.joining(", "));
    }

    /**
     * Overloaded convenience method accepting attribute map for structured serialization.
     *
     * @param user       the username or principal performing the operation.
     * @param action     the auditable action verb.
     * @param target     the target entity reference.
     * @param detailsMap key-value pairs describing the event.
     */
    public void logAction(String user, String action, String target, Map<String, ?> detailsMap) {
        logAction(user, action, target, formatDetails(detailsMap));
    }
}
