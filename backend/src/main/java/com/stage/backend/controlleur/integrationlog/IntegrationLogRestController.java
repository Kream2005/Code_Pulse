package com.stage.backend.controlleur.integrationlog;

import com.stage.backend.dto.integrationlog.IntegrationLogResponse;
import com.stage.backend.enums.StatutLog;
import com.stage.backend.enums.TypeLog;
import com.stage.backend.security.SecurityRoles;
import com.stage.backend.service.integrationlog.IntegrationLogService;
import com.stage.backend.util.PaginationUtils;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.List;

@RestController
@RequestMapping("/integration-logs")
@RequiredArgsConstructor
@Validated
public class IntegrationLogRestController {

    private final IntegrationLogService service;

    @GetMapping("/get-integration-log/{id}")
    @PreAuthorize(SecurityRoles.READ_LOGS)
    public ResponseEntity<IntegrationLogResponse> getIntegrationLog(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(service.getIntegrationLog(id));
    }

    @GetMapping("/get-all-integration-logs")
    @PreAuthorize(SecurityRoles.READ_LOGS)
    public ResponseEntity<List<IntegrationLogResponse>> getAllIntegrationLogs() {
        return ResponseEntity.ok(service.getAllIntegrationLogs());
    }

    @GetMapping("/get-integration-logs-pages/page")
    @PreAuthorize(SecurityRoles.READ_LOGS)
    public ResponseEntity<Page<IntegrationLogResponse>> getIntegrationLogsPage(
            @RequestParam @Min(1) int page,
            @RequestParam @Min(1) int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) TypeLog type,
            @RequestParam(required = false) StatutLog statut
    ) {
        return ResponseEntity.ok(service.searchIntegrationLogs(
                q, type, statut, PaginationUtils.toSpringPageIndex(page), size
        ));
    }

    @GetMapping("/get-integration-logs-by-type")
    @PreAuthorize(SecurityRoles.READ_LOGS)
    public ResponseEntity<List<IntegrationLogResponse>> getIntegrationLogsByType(
            @RequestParam TypeLog type
    ) {
        return ResponseEntity.ok(service.getIntegrationLogsByType(type));
    }

    @GetMapping("/get-integration-logs-by-statut")
    @PreAuthorize(SecurityRoles.READ_LOGS)
    public ResponseEntity<List<IntegrationLogResponse>> getIntegrationLogsByStatut(
            @RequestParam StatutLog statut
    ) {
        return ResponseEntity.ok(service.getIntegrationLogsByStatut(statut));
    }

    @GetMapping("/get-integration-logs-by-coding-challenge")
    @PreAuthorize(SecurityRoles.READ_LOGS)
    public ResponseEntity<List<IntegrationLogResponse>> getIntegrationLogsByCodingChallenge(
            @RequestParam @NotNull Long codingChallengeId
    ) {
        return ResponseEntity.ok(service.getIntegrationLogsByCodingChallenge(codingChallengeId));
    }

    @GetMapping("/get-integration-logs-by-between-dates")
    @PreAuthorize(SecurityRoles.READ_LOGS)
    public ResponseEntity<List<IntegrationLogResponse>> getIntegrationLogsBetweenDates(
            @RequestParam ZonedDateTime startDate,
            @RequestParam ZonedDateTime endDate
    ) {
        return ResponseEntity.ok(service.getIntegrationLogsBetweenDates(startDate, endDate));
    }

    @GetMapping("/get-last-integration-log")
    @PreAuthorize(SecurityRoles.READ_LOGS)
    public ResponseEntity<IntegrationLogResponse> getLastIntegrationLog() {
        return ResponseEntity.ok(service.getLastIntegrationLog());
    }

    @GetMapping("/count")
    @PreAuthorize(SecurityRoles.READ_LOGS)
    public ResponseEntity<Long> countIntegrationLogs() {
        return ResponseEntity.ok(service.countIntegrationLogs());
    }

    @GetMapping("/count/type")
    @PreAuthorize(SecurityRoles.READ_LOGS)
    public ResponseEntity<Long> countIntegrationLogsByType(
            @RequestParam TypeLog type
    ) {
        return ResponseEntity.ok(service.countIntegrationLogsByType(type));
    }

    @GetMapping("/count/statut")
    @PreAuthorize(SecurityRoles.READ_LOGS)
    public ResponseEntity<Long> countIntegrationLogsByStatut(
            @RequestParam StatutLog statut
    ) {
        return ResponseEntity.ok(service.countIntegrationLogsByStatut(statut));
    }

    @GetMapping("/count/failed")
    @PreAuthorize(SecurityRoles.READ_LOGS)
    public ResponseEntity<Long> countFailedIntegrations() {
        return ResponseEntity.ok(service.countFailedIntegrations());
    }

    @GetMapping("/exists")
    @PreAuthorize(SecurityRoles.READ_LOGS)
    public ResponseEntity<Boolean> existsById(
            @RequestParam @NotNull Long logId
    ) {
        return ResponseEntity.ok(service.existsById(logId));
    }
}
