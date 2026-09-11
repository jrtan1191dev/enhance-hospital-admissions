package com.hospital.admissions.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * JPA Auditing configuration to automatically populate {@code createdBy} and {@code lastModifiedBy}
 * entity properties using the current Spring Security context.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditConfig {

    /**
     * Resolves the current authenticated auditor username from {@link SecurityContextHolder}.
     *
     * @return {@link AuditorAware} provider returning the authenticated username, or empty if anonymous.
     */
    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getName)
                .filter(name -> !name.isBlank() && !"anonymousUser".equalsIgnoreCase(name));
    }
}
