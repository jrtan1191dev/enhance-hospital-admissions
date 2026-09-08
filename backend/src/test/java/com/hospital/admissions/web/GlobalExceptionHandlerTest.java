package com.hospital.admissions.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;
    private ListAppender<ILoggingEvent> listAppender;
    private Logger logger;

    static class TestBody {
        @NotNull
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @RestController
    static class TestFaultyController {
        @GetMapping("/test/not-found")
        public void throwNotFound() {
            throw new IllegalArgumentException("Patient not found: P999");
        }

        @GetMapping("/test/bad-argument")
        public void throwBadArgument() {
            throw new IllegalArgumentException("Invalid parameter value provided");
        }

        @GetMapping("/test/conflict")
        public void throwConflict() {
            throw new IllegalStateException("Bed 8A-01 is not EMPTY_CLEANED");
        }

        @GetMapping("/test/unsupported")
        public void throwUnsupported() {
            throw new UnsupportedOperationException("Integration gateway not configured");
        }

        @GetMapping("/test/unhandled")
        public void throwUnhandled() {
            throw new RuntimeException("Unexpected database crash");
        }

        @PostMapping("/test/validation")
        public void throwValidation(@Valid @RequestBody TestBody body) {
        }

        @GetMapping("/test/constraint-violation")
        public void throwConstraintViolation() {
            throw new ConstraintViolationException("Parameter invalid", java.util.Set.of());
        }
    }

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders
                .standaloneSetup(new TestFaultyController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        this.logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        this.listAppender = new ListAppender<>();
        this.listAppender.start();
        this.logger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        if (this.logger != null && this.listAppender != null) {
            this.logger.detachAppender(listAppender);
        }
    }

    @Test
    @DisplayName("IllegalArgumentException with 'not found' returns 404 ProblemDetail")
    void testIllegalArgumentNotFound() throws Exception {
        mockMvc.perform(get("/test/not-found").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").value("Patient not found: P999"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("IllegalArgumentException without 'not found' returns 400 ProblemDetail")
    void testIllegalArgumentBadRequest() throws Exception {
        mockMvc.perform(get("/test/bad-argument").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("Invalid parameter value provided"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("IllegalStateException returns 409 Conflict ProblemDetail")
    void testIllegalStateConflict() throws Exception {
        mockMvc.perform(get("/test/conflict").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.detail").value("Bed 8A-01 is not EMPTY_CLEANED"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("UnsupportedOperationException returns 501 Not Implemented ProblemDetail")
    void testUnsupportedOperation() throws Exception {
        mockMvc.perform(get("/test/unsupported").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.status").value(501))
                .andExpect(jsonPath("$.title").value("Not Implemented"))
                .andExpect(jsonPath("$.detail").value("Integration gateway not configured"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Unhandled Exception returns 500 ProblemDetail without leaking raw stack trace")
    void testUnhandledException() throws Exception {
        mockMvc.perform(get("/test/unhandled").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.title").value("Internal Server Error"))
                .andExpect(jsonPath("$.detail").value("An unexpected internal error occurred. Please contact the system administrator."))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Authenticated user diagnostic logging captures user identity and URI with query parameters")
    void testAuthenticatedUserDiagnosticLogging() throws Exception {
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        "dr_tan_ed", "secret", java.util.List.of()));
        try {
            mockMvc.perform(get("/test/not-found?reason=urgent").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));

            assertThat(listAppender.list)
                    .anyMatch(event -> event.getLevel() == Level.WARN
                            && event.getFormattedMessage().contains("[CLIENT_ERROR]")
                            && event.getFormattedMessage().contains("dr_tan_ed")
                            && event.getFormattedMessage().contains("GET /test/not-found?reason=urgent"));
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("MethodArgumentNotValidException produces 400 ProblemDetail and logs [CLIENT_ERROR]")
    void testMethodArgumentNotValidExceptionLogsClientError() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.timestamp").exists());

        assertThat(listAppender.list)
                .anyMatch(event -> event.getLevel() == Level.WARN
                        && event.getFormattedMessage().contains("[CLIENT_ERROR]")
                        && event.getFormattedMessage().contains("400")
                        && event.getFormattedMessage().contains("MethodArgumentNotValidException")
                        && event.getFormattedMessage().contains("POST /test/validation"));
    }

    @Test
    @DisplayName("HttpMessageNotReadableException produces 400 ProblemDetail and logs [CLIENT_ERROR]")
    void testHttpMessageNotReadableExceptionLogsClientError() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ invalid json }")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.timestamp").exists());

        assertThat(listAppender.list)
                .anyMatch(event -> event.getLevel() == Level.WARN
                        && event.getFormattedMessage().contains("[CLIENT_ERROR]")
                        && event.getFormattedMessage().contains("400")
                        && event.getFormattedMessage().contains("HttpMessageNotReadableException")
                        && event.getFormattedMessage().contains("POST /test/validation"));
    }

    @Test
    @DisplayName("ConstraintViolationException produces 400 ProblemDetail and logs [CLIENT_ERROR]")
    void testConstraintViolationExceptionLogsClientError() throws Exception {
        mockMvc.perform(get("/test/constraint-violation")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("Parameter invalid"))
                .andExpect(jsonPath("$.timestamp").exists());

        assertThat(listAppender.list)
                .anyMatch(event -> event.getLevel() == Level.WARN
                        && event.getFormattedMessage().contains("[CLIENT_ERROR]")
                        && event.getFormattedMessage().contains("400")
                        && event.getFormattedMessage().contains("ConstraintViolationException")
                        && event.getFormattedMessage().contains("GET /test/constraint-violation"));
    }
}
