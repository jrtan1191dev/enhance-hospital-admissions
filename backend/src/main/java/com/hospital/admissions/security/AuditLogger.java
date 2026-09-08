package com.hospital.admissions.security;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AuditLogger {

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

    public static String formatDetails(Map<String, ?> detailsMap) {
        if (detailsMap == null || detailsMap.isEmpty()) {
            return "";
        }
        return detailsMap.entrySet().stream()
                .map(e -> e.getKey() + "=" + (e.getValue() != null ? e.getValue().toString() : "null"))
                .collect(Collectors.joining(", "));
    }

    public void logAction(String user, String action, String target, Map<String, ?> detailsMap) {
        logAction(user, action, target, formatDetails(detailsMap));
    }
}
