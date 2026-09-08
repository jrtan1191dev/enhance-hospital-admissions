package com.hospital.admissions.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JpaAuditConfigTest {

    private final JpaAuditConfig config = new JpaAuditConfig();
    private final AuditorAware<String> auditorAware = config.auditorAware();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Returns auditor username when authentication is authenticated")
    void testAuditorAware_AuthenticatedUser() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "dr_tan_ed", "creds", AuthorityUtils.createAuthorityList("ROLE_ED_ATTENDING")
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        Optional<String> auditor = auditorAware.getCurrentAuditor();
        assertThat(auditor).contains("dr_tan_ed");
    }

    @Test
    @DisplayName("Returns empty when no authentication in security context")
    void testAuditorAware_NoAuthentication() {
        SecurityContextHolder.clearContext();

        Optional<String> auditor = auditorAware.getCurrentAuditor();
        assertThat(auditor).isEmpty();
    }

    @Test
    @DisplayName("Returns empty when authentication is unauthenticated")
    void testAuditorAware_Unauthenticated() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "unauthenticated_user", "creds"
        );
        auth.setAuthenticated(false);
        SecurityContextHolder.getContext().setAuthentication(auth);

        Optional<String> auditor = auditorAware.getCurrentAuditor();
        assertThat(auditor).isEmpty();
    }

    @Test
    @DisplayName("Returns empty when authentication is anonymousUser")
    void testAuditorAware_AnonymousUser() {
        Authentication anonymousAuth = new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")
        );
        SecurityContextHolder.getContext().setAuthentication(anonymousAuth);

        Optional<String> auditor = auditorAware.getCurrentAuditor();
        assertThat(auditor).isEmpty();
    }

    @Test
    @DisplayName("Returns empty when username is blank")
    void testAuditorAware_BlankUsername() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "   ", "creds", AuthorityUtils.createAuthorityList("ROLE_USER")
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        Optional<String> auditor = auditorAware.getCurrentAuditor();
        assertThat(auditor).isEmpty();
    }
}
