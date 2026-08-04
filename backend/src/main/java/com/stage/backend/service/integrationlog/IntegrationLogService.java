package com.stage.backend.service.integrationlog;

import com.stage.backend.dto.integrationlog.CreateIntegrationLogRequest;
import com.stage.backend.dto.integrationlog.IntegrationLogResponse;
import com.stage.backend.enums.StatutLog;
import com.stage.backend.enums.TypeLog;
import org.springframework.data.domain.Page;

import java.time.ZonedDateTime;
import java.util.List;

public interface IntegrationLogService {
    public IntegrationLogResponse ajouterIntegrationLog(CreateIntegrationLogRequest request);

    IntegrationLogResponse getIntegrationLog(Long logId);

    List<IntegrationLogResponse> getAllIntegrationLogs();

    Page<IntegrationLogResponse> getIntegrationLogsPage(int page, int size);

    List<IntegrationLogResponse> getIntegrationLogsByType(TypeLog type);

    List<IntegrationLogResponse> getIntegrationLogsByStatut(StatutLog statut);

    List<IntegrationLogResponse> getIntegrationLogsByCodingChallenge(Long codingChallengeId);

    List<IntegrationLogResponse> getIntegrationLogsBetweenDates(ZonedDateTime startDate, ZonedDateTime endDate);

    IntegrationLogResponse getLastIntegrationLog();

    long countIntegrationLogs();

    long countIntegrationLogsByType(TypeLog type);

    long countIntegrationLogsByStatut(StatutLog statut);

    long countFailedIntegrations();

    boolean existsById(Long logId);

    void logSyncEvent(Long codingChallengeId, StatutLog statut, String message);

    void logEvent(TypeLog type, StatutLog statut, String message, Long codingChallengeId);
}