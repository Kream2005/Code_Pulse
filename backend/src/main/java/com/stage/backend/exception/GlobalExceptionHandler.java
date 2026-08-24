package com.stage.backend.exception;

import com.stage.backend.dto.common.ErreurValidation;
import com.stage.backend.enums.StatutLog;
import com.stage.backend.enums.TypeLog;
import com.stage.backend.kafka.exception.InvalidCodingChallengeEventException;
import com.stage.backend.service.integrationlog.IntegrationLogService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final IntegrationLogService integrationLogService;

    @ExceptionHandler(ErreurAuthentificationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, Object> handleErreurAuthentification(
            ErreurAuthentificationException ex,
            HttpServletRequest request
    ) {
        Map<String, Object> body = error(HttpStatus.UNAUTHORIZED, ex.getMessage(), request.getRequestURI());
        body.put("codeErreur", ex.getCodeErreur());
        return body;
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, Object> handleBadCredentials(
            BadCredentialsException ex,
            HttpServletRequest request
    ) {
        String message = ex.getMessage() != null && !ex.getMessage().isBlank()
                ? ex.getMessage()
                : "Identifiants invalides";
        Map<String, Object> body = error(HttpStatus.UNAUTHORIZED, message, request.getRequestURI());
        body.put("codeErreur", "IDENTIFIANTS_INVALIDES");
        return body;
    }

    @ExceptionHandler(ErreurMetierException.class)
    public ResponseEntity<Map<String, Object>> handleErreurMetier(
            ErreurMetierException ex,
            HttpServletRequest request
    ) {
        Map<String, Object> body = error(ex.getStatut(), ex.getMessage(), request.getRequestURI());
        body.put("codeErreur", ex.getCodeErreur());
        return ResponseEntity.status(ex.getStatut()).body(body);
    }

    @ExceptionHandler(FeedbackValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleFeedbackValidation(
            FeedbackValidationException ex,
            HttpServletRequest request
    ) {
        Map<String, Object> body = error(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
        body.put("codeErreur", "VALIDATION_FEEDBACK_ECHOUEE");
        body.put("valide", false);
        body.put("erreurs", ex.getErreurs());
        return body;
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
                "Accès refusé : " + request.getMethod() + " " + request.getRequestURI(),
                null
        );
        Map<String, Object> body = error(HttpStatus.FORBIDDEN, "Accès refusé", request.getRequestURI());
        body.put("codeErreur", "ACCES_REFUSE");
        return body;
    }

    @ExceptionHandler(InvalidCodingChallengeEventException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleInvalidChallengeEvent(
            InvalidCodingChallengeEventException ex,
            HttpServletRequest request
    ) {
        Map<String, Object> body = error(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
        body.put("codeErreur", "ERREUR_VALIDATION");
        return body;
    }

    @ExceptionHandler(ChallengeIngestConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, Object> handleIngestConflict(
            ChallengeIngestConflictException ex,
            HttpServletRequest request
    ) {
        Map<String, Object> body = error(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI());
        body.put("codeErreur", ex.getErrorCode());
        body.put("userExterneEntrantId", ex.getIncomingUserExternalId());
        body.put("emailEntrant", ex.getIncomingEmail());
        body.put("utilisateurExistantId", ex.getExistingUserId());
        body.put("userExterneExistantId", ex.getExistingUserExternalId());
        body.put("emailExistant", ex.getExistingEmail());
        return body;
    }

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleEntityNotFound(
            EntityNotFoundException ex,
            HttpServletRequest request
    ) {
        Map<String, Object> body = error(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
        body.put("codeErreur", "ENTITE_INTROUVABLE");
        return body;
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(
            ResponseStatusException ex,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String message = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();
        Map<String, Object> body = error(status, message, request.getRequestURI());
        body.put("codeErreur", "ERREUR_METIER");
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, String> erreursChamps = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            erreursChamps.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        Map<String, Object> body = error(HttpStatus.BAD_REQUEST, "Échec de la validation des données", request.getRequestURI());
        body.put("codeErreur", "ERREUR_VALIDATION");
        body.put("erreursChamps", erreursChamps);
        return body;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        Map<String, String> erreursChamps = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        v -> v.getPropertyPath().toString(),
                        ConstraintViolation::getMessage,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
        Map<String, Object> body = error(
                HttpStatus.BAD_REQUEST,
                "Paramètres de requête invalides",
                request.getRequestURI()
        );
        body.put("codeErreur", "ERREUR_VALIDATION");
        body.put("erreursChamps", erreursChamps);
        return body;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleMalformedJson(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        Map<String, Object> body = error(
                HttpStatus.BAD_REQUEST,
                "Corps JSON mal formé ou illisible",
                request.getRequestURI()
        );
        body.put("codeErreur", "JSON_MAL_FORME");
        body.put("detail", ex.getMostSpecificCause().getMessage());
        return body;
    }

    @ExceptionHandler(RestClientException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public Map<String, Object> handleRestClient(
            RestClientException ex,
            HttpServletRequest request
    ) {
        Map<String, Object> body = error(
                HttpStatus.BAD_GATEWAY,
                "Échec de l'appel à l'API externe : " + ex.getMessage(),
                request.getRequestURI()
        );
        body.put("codeErreur", "ERREUR_API_EXTERNE");
        return body;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, Object> handleDataIntegrity(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        Map<String, Object> body = error(
                HttpStatus.CONFLICT,
                "Violation de contrainte en base de données (doublon ou référence invalide)",
                request.getRequestURI()
        );
        body.put("codeErreur", "VIOLATION_CONTRAINTE");
        body.put("detail", ex.getMostSpecificCause().getMessage());
        return body;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> handleGenericException(
            Exception ex,
            HttpServletRequest request
    ) {
        System.err.println("Erreur non gérée sur " + request.getRequestURI() + " : " + ex);
        ex.printStackTrace(System.err);
        try {
            integrationLogService.logEvent(
                    TypeLog.AUTH,
                    StatutLog.ERREUR,
                    "Erreur non gérée sur " + request.getRequestURI() + " : "
                            + ex.getClass().getSimpleName() + " - " + ex.getMessage(),
                    null
            );
        } catch (Exception ignored) {
            // ne jamais échouer en signalant une erreur
        }
        Map<String, Object> body = error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Erreur serveur inattendue",
                request.getRequestURI()
        );
        body.put("codeErreur", "ERREUR_INTERNE");
        return body;
    }

    private Map<String, Object> error(HttpStatus status, String message, String path) {
        Map<String, Object> body = new HashMap<>();
        body.put("horodatage", Instant.now().toString());
        body.put("statut", status.value());
        body.put("erreur", status.getReasonPhrase());
        body.put("message", message);
        body.put("chemin", path);
        return body;
    }
}
