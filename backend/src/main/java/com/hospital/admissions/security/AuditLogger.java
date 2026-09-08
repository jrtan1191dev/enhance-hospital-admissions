package com.hospital.admissions.security;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

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
}
