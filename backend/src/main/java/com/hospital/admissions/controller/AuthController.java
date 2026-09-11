package com.hospital.admissions.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for authentication and user persona discovery.
 * <p>
 * Exposes the currently active security principal, authenticated status,
 * and assigned Spring Security authorities.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    /**
     * Retrieves the currently active user session and security context.
     *
     * @return {@link ResponseEntity} containing a map with authentication status,
     *         username, and list of granted roles/authorities.
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.ok(Map.of("authenticated", false));
        }

        return ResponseEntity.ok(Map.of(
                "authenticated", true,
                "username", auth.getName(),
                "roles", auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList())
        ));
    }
}
