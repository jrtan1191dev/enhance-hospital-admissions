package com.hospital.admissions.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Global REST exception handler producing RFC 7807 ProblemDetail representations.
 * Logs structured diagnostics (Who, What, Where) using Spring Security Context and HttpServletRequest.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Initializes the global exception handler with static message resolution.
     */
    public GlobalExceptionHandler() {
        setMessageSource(new org.springframework.context.support.StaticMessageSource());
    }

    /**
     * Handles internal Spring MVC framework exceptions with structured logging and timestamp injection.
     *
     * @param ex         the exception to handle.
     * @param body       the optional response body.
     * @param headers    the HTTP headers to return.
     * @param statusCode the HTTP status code.
     * @param request    the current web request.
     * @return {@link ResponseEntity} containing the formatted response entity.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex,
            @Nullable Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request) {

        HttpServletRequest servletRequest = (request instanceof ServletWebRequest servletWebRequest)
                ? servletWebRequest.getRequest()
                : null;

        logDiagnostic(statusCode, ex, servletRequest);

        ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, statusCode, request);
        if (response != null && response.getBody() instanceof ProblemDetail problem) {
            if (problem.getProperties() == null || !problem.getProperties().containsKey("timestamp")) {
                problem.setProperty("timestamp", Instant.now());
            }
        }
        return response;
    }

    /**
     * Handles hard clinical safety invariant violations (e.g., infectious cohorting or gender mismatch).
     *
     * @param ex      the {@link com.hospital.admissions.exception.SafetyInvariantViolationException}.
     * @param request the current HTTP servlet request.
     * @return RFC 7807 {@link ProblemDetail} with HTTP 422 Unprocessable Entity.
     */
    @ExceptionHandler(com.hospital.admissions.exception.SafetyInvariantViolationException.class)
    public ProblemDetail handleSafetyInvariantViolation(com.hospital.admissions.exception.SafetyInvariantViolationException ex, HttpServletRequest request) {
        logDiagnostic(HttpStatus.UNPROCESSABLE_ENTITY, ex, request);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setTitle("Safety Invariant Violation");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    /**
     * Handles bean validation constraint violations on method arguments.
     *
     * @param ex      the {@link ConstraintViolationException}.
     * @param request the current HTTP servlet request.
     * @return RFC 7807 {@link ProblemDetail} with HTTP 400 Bad Request.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        logDiagnostic(HttpStatus.BAD_REQUEST, ex, request);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle(HttpStatus.BAD_REQUEST.getReasonPhrase());
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    /**
     * Handles illegal argument exceptions, translating not-found conditions into HTTP 404 or HTTP 400.
     *
     * @param ex      the {@link IllegalArgumentException}.
     * @param request the current HTTP servlet request.
     * @return RFC 7807 {@link ProblemDetail} with appropriate status code.
     */
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

    /**
     * Handles invalid state transitions and resource conflicts.
     *
     * @param ex      the {@link IllegalStateException}.
     * @param request the current HTTP servlet request.
     * @return RFC 7807 {@link ProblemDetail} with HTTP 409 Conflict.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        logDiagnostic(HttpStatus.CONFLICT, ex, request);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Conflict");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    /**
     * Handles concurrent database update conflicts and optimistic lock failures.
     *
     * @param ex      the optimistic locking exception.
     * @param request the current HTTP servlet request.
     * @return RFC 7807 {@link ProblemDetail} with HTTP 409 Conflict.
     */
    @ExceptionHandler({
        jakarta.persistence.OptimisticLockException.class,
        org.springframework.orm.ObjectOptimisticLockingFailureException.class
    })
    public ProblemDetail handleOptimisticLock(Exception ex, HttpServletRequest request) {
        logDiagnostic(HttpStatus.CONFLICT, ex, request);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "The resource was updated concurrently by another user. Please refresh and try again.");
        problem.setTitle("Conflict");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    /**
     * Handles role-based access control and security authorization rejections.
     *
     * @param ex      the {@link org.springframework.security.access.AccessDeniedException}.
     * @param request the current HTTP servlet request.
     * @return RFC 7807 {@link ProblemDetail} with HTTP 403 Forbidden.
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(org.springframework.security.access.AccessDeniedException ex, HttpServletRequest request) {
        logDiagnostic(HttpStatus.FORBIDDEN, ex, request);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setTitle("Forbidden");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    /**
     * Handles calls to features not yet implemented or active in the current profile.
     *
     * @param ex      the {@link UnsupportedOperationException}.
     * @param request the current HTTP servlet request.
     * @return RFC 7807 {@link ProblemDetail} with HTTP 501 Not Implemented.
     */
    @ExceptionHandler(UnsupportedOperationException.class)
    public ProblemDetail handleUnsupportedOperation(UnsupportedOperationException ex, HttpServletRequest request) {
        logDiagnostic(HttpStatus.NOT_IMPLEMENTED, ex, request);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_IMPLEMENTED, ex.getMessage());
        problem.setTitle("Not Implemented");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    /**
     * Fallback handler for uncaught server-side runtime exceptions.
     *
     * @param ex      the unhandled exception.
     * @param request the current HTTP servlet request.
     * @return RFC 7807 {@link ProblemDetail} with HTTP 500 Internal Server Error.
     */
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

    private void logDiagnostic(HttpStatusCode statusCode, Exception ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        String reasonPhrase = (status != null) ? status.getReasonPhrase() : statusCode.toString();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String who = (auth != null && auth.isAuthenticated() && auth.getName() != null)
                ? auth.getName()
                : "ANONYMOUS";

        String where = "UNKNOWN";
        if (request != null) {
            String query = request.getQueryString();
            where = request.getMethod() + " " + request.getRequestURI() + (query != null ? "?" + query : "");
        }

        String details = (ex.getMessage() != null) ? ex.getMessage() : "No message provided";
        String what = statusCode.value() + " " + reasonPhrase
                + " (" + ex.getClass().getSimpleName() + "): " + details;

        if (statusCode.is5xxServerError()) {
            log.error("[SERVER_ERROR] who=\"{}\" where=\"{}\" what=\"{}\"", who, where, what, ex);
        } else {
            log.warn("[CLIENT_ERROR] who=\"{}\" where=\"{}\" what=\"{}\"", who, where, what);
        }
    }
}
