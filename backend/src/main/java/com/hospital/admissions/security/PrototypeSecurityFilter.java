package com.hospital.admissions.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@Profile("prototype")
public class PrototypeSecurityFilter extends OncePerRequestFilter {

    private static final Map<String, String> ROLE_TO_USER = Map.of(
            "ED_ATTENDING", "dr_tan_ed",
            "SPECIALIST", "dr_lim_cardio",
            "BMU_COORDINATOR", "bmu_coord_wong",
            "PATIENT", "patient_p101",
            "WARD_NURSE", "nurse_sarah",
            "HOUSEKEEPING", "evs_staff_kumar"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String roleHeader = request.getHeader("X-User-Role");
        if (roleHeader == null || roleHeader.isBlank()) {
            roleHeader = "BMU_COORDINATOR"; // Default evaluator fallback in prototype mode
        }

        String normalizedRole = roleHeader.toUpperCase().trim();
        String username = ROLE_TO_USER.getOrDefault(normalizedRole, "bmu_coord_wong");
        String springRole = "ROLE_" + normalizedRole;

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                username,
                null,
                List.of(new SimpleGrantedAuthority(springRole))
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        MDC.put("activeUser", username);
        MDC.put("activeRole", springRole);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("activeUser");
            MDC.remove("activeRole");
        }
    }
}
