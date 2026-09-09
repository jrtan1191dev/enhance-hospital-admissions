package com.hospital.admissions.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class SafetyInvariantViolationException extends RuntimeException {
    public SafetyInvariantViolationException(String message) {
        super(message);
    }
}
