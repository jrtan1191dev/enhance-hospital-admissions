package com.hospital.admissions.web;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Global REST exception handler producing RFC 7807 ProblemDetail representations.
 * Logs structured diagnostics (Who, What, Where) using Spring Security Context and HttpServletRequest.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        HttpStatus status = (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("not found"))
                ? HttpStatus.NOT_FOUND
                : HttpStatus.BAD_REQUEST;

        logDiagnostic(status, ex, request);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problem.setTitle(status.getReasonPhrase());
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        logDiagnostic(HttpStatus.CONFLICT, ex, request);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Conflict");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ProblemDetail handleUnsupportedOperation(UnsupportedOperationException ex, HttpServletRequest request) {
        logDiagnostic(HttpStatus.NOT_IMPLEMENTED, ex, request);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_IMPLEMENTED, ex.getMessage());
        problem.setTitle("Not Implemented");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnhandledException(Exception ex, HttpServletRequest request) {
        logDiagnostic(HttpStatus.INTERNAL_SERVER_ERROR, ex, request);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected internal error occurred. Please contact the system administrator.");
        problem.setTitle("Internal Server Error");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    private void logDiagnostic(HttpStatus status, Exception ex, HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String who = (auth != null && auth.isAuthenticated() && auth.getName() != null)
                ? auth.getName()
                : "ANONYMOUS";

        String where = "UNKNOWN";
        if (request != null) {
            String query = request.getQueryString();
            where = request.getMethod() + " " + request.getRequestURI() + (query != null ? "?" + query : "");
        }

        String what = status.value() + " " + status.getReasonPhrase()
                + " (" + ex.getClass().getSimpleName() + "): " + ex.getMessage();

        if (status.is5xxServerError()) {
            log.error("[SERVER_ERROR] who=\"{}\" where=\"{}\" what=\"{}\"", who, where, what, ex);
        } else {
            log.warn("[CLIENT_ERROR] who=\"{}\" where=\"{}\" what=\"{}\"", who, where, what);
        }
    }
}
