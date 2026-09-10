package com.hospital.admissions.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/**
 * Prototype security configuration — active only under @Profile("prototype").
 * Permits all API access with header-driven persona switching via PrototypeSecurityFilter.
 * Enables H2 console access and relaxed CSRF for local development.
 */
@Configuration
@EnableWebSecurity
@Profile("prototype")
public class PrototypeSecurityConfig {

    private final PrototypeSecurityFilter prototypeSecurityFilter;

    public PrototypeSecurityConfig(PrototypeSecurityFilter prototypeSecurityFilter) {
        this.prototypeSecurityFilter = prototypeSecurityFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName(null);

        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(requestHandler)
                        .ignoringRequestMatchers("/h2-console/**")
                )
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/h2-console/**", "/actuator/**", "/api/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(prototypeSecurityFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
