package com.hospital.admissions.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when an attempted bed allocation violates non-negotiable patient safety invariants
 * (such as biological gender cohorting in multi-bed rooms or placing infectious patients in non-isolated rooms).
 * Maps to HTTP status 422 Unprocessable Entity.
 */
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class SafetyInvariantViolationException extends RuntimeException {

    /**
     * Constructs a safety invariant violation exception with a detailed clinical explanation.
     *
     * @param message narrative explaining which safety rule was violated.
     */
    public SafetyInvariantViolationException(String message) {
        super(message);
    }
}
