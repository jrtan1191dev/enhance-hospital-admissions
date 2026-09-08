package com.hospital.admissions.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrototypeSecurityFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private PrototypeSecurityFilter filter;

    @BeforeEach
    void setUp() {
        filter = new PrototypeSecurityFilter();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test
    @DisplayName("doFilterInternal sets authentication and MDC for valid role header")
    void testDoFilterInternal_ValidRole() throws ServletException, IOException {
        when(request.getHeader("X-User-Role")).thenReturn("ED_ATTENDING");

        doAnswer(invocation -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNotNull();
            assertThat(auth.getName()).isEqualTo("dr_tan_ed");
            assertThat(auth.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_ED_ATTENDING"));
            assertThat(MDC.get("activeUser")).isEqualTo("dr_tan_ed");
            assertThat(MDC.get("activeRole")).isEqualTo("ROLE_ED_ATTENDING");
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilterInternal(request, response, filterChain);

        // Verify MDC cleanup
        assertThat(MDC.get("activeUser")).isNull();
        assertThat(MDC.get("activeRole")).isNull();
    }

    @Test
    @DisplayName("doFilterInternal falls back to BMU_COORDINATOR when header is missing or blank")
    void testDoFilterInternal_MissingHeader() throws ServletException, IOException {
        when(request.getHeader("X-User-Role")).thenReturn(null);

        doAnswer(invocation -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth.getName()).isEqualTo("bmu_coord_wong");
            assertThat(auth.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_BMU_COORDINATOR"));
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilterInternal(request, response, filterChain);
    }

    @Test
    @DisplayName("doFilterInternal handles unknown role gracefully")
    void testDoFilterInternal_UnknownRole() throws ServletException, IOException {
        when(request.getHeader("X-User-Role")).thenReturn("UNKNOWN_ROLE");

        doAnswer(invocation -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth.getName()).isEqualTo("bmu_coord_wong");
            assertThat(auth.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_UNKNOWN_ROLE"));
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilterInternal(request, response, filterChain);
    }
}
