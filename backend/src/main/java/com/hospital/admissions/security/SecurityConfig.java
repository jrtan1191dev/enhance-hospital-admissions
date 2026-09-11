package com.hospital.admissions.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/**
 * Default security configuration — active when no prototype profile is set.
 * Denies all API access without valid authentication.
 * Production deployment requires implementing OAuth2/JWT via Spring Security's
 * spring-boot-starter-oauth2-resource-server library.
 */
@Configuration
@EnableWebSecurity
@Profile("!prototype")
public class SecurityConfig {

    /**
     * Configures the production security filter chain with CSRF protection and authenticated API restrictions.
     *
     * @param http the {@link HttpSecurity} builder.
     * @return the configured {@link SecurityFilterChain}.
     * @throws Exception in case of configuration errors.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName(null);

        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(requestHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}
