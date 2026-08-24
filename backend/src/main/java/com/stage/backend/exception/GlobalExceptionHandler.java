package com.stage.backend.exception;

import com.stage.backend.enums.StatutLog;
import com.stage.backend.enums.TypeLog;
import com.stage.backend.service.integrationlog.IntegrationLogService;
import com.stage.backend.kafka.exception.InvalidCodingChallengeEventException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final IntegrationLogService integrationLogService;

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, Object> handleBadCredentials(
            BadCredentialsException ex,
            HttpServletRequest request
    ) {
        String message = ex.getMessage() != null && !ex.getMessage().isBlank()
                ? ex.getMessage()
                : "Invalid credentials";
        return error(HttpStatus.UNAUTHORIZED, message, request.getRequestURI());
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, Object> handleAccessDenied(
            AuthorizationDeniedException ex,
            HttpServletRequest request
    ) {
        integrationLogService.logEvent(
                TypeLog.AUTHORISATION,
                StatutLog.WARNING,
                "Access denied: " + request.getMethod() + " " + request.getRequestURI(),
                null
        );
        return error(HttpStatus.FORBIDDEN, "Access denied", request.getRequestURI());
    }

    @ExceptionHandler(InvalidCodingChallengeEventException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleInvalidChallengeEvent(
            InvalidCodingChallengeEventException ex,
            HttpServletRequest request
    ) {
        Map<String, Object> body = error(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
        body.put("errorCode", "VALIDATION_ERROR");
        return body;
    }

    @ExceptionHandler(ChallengeIngestConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, Object> handleIngestConflict(
            ChallengeIngestConflictException ex,
            HttpServletRequest request
    ) {
        Map<String, Object> body = error(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI());
        body.put("errorCode", ex.getErrorCode());
        body.put("incomingUserExternalId", ex.getIncomingUserExternalId());
        body.put("incomingEmail", ex.getIncomingEmail());
        body.put("existingUserId", ex.getExistingUserId());
        body.put("existingUserExternalId", ex.getExistingUserExternalId());
        body.put("existingEmail", ex.getExistingEmail());
        return body;
    }

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleEntityNotFound(
            EntityNotFoundException ex,
            HttpServletRequest request
    ) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(
            ResponseStatusException ex,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String message = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();
        return ResponseEntity.status(status).body(error(status, message, request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        Map<String, Object> body = error(HttpStatus.BAD_REQUEST, "Validation failed", request.getRequestURI());
        body.put("fieldErrors", fieldErrors);
        return body;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> handleGenericException(
            Exception ex,
            HttpServletRequest request
    ) {
        System.err.println("Unhandled error on " + request.getRequestURI() + ": " + ex);
        ex.printStackTrace(System.err);
        try {
            integrationLogService.logEvent(
                    TypeLog.AUTH,
                    StatutLog.ERREUR,
                    "Unhandled error on " + request.getRequestURI() + ": "
                            + ex.getClass().getSimpleName() + " - " + ex.getMessage(),
                    null
            );
        } catch (Exception ignored) {
            // never fail while reporting a failure
        }
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error", request.getRequestURI());
    }

    private Map<String, Object> error(HttpStatus status, String message, String path) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", path);
        return body;
    }
}
