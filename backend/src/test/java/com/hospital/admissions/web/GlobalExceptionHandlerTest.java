package com.hospital.admissions.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

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
    }

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders
                .standaloneSetup(new TestFaultyController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
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
}
