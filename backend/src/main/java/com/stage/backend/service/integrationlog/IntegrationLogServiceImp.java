package com.stage.backend.service.integrationlog;

import com.stage.backend.dto.integrationlog.CreateIntegrationLogRequest;
import com.stage.backend.dto.integrationlog.IntegrationLogResponse;
import com.stage.backend.entity.CodingChallenge;
import com.stage.backend.entity.IntegrationLog;
import com.stage.backend.enums.StatutLog;
import com.stage.backend.enums.TypeLog;
import com.stage.backend.mapper.IntegrationLogMapper;
import com.stage.backend.repository.IntegrationLogRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class IntegrationLogServiceImp implements IntegrationLogService {

    private final IntegrationLogRepository repository;
    private final IntegrationLogMapper mapper;
    private final EntityManager manager;

    @Override
    public IntegrationLogResponse ajouterIntegrationLog(CreateIntegrationLogRequest request) {
        log.info("Creating integration log");

        IntegrationLog integrationLog = mapper.toEntity(request);
        IntegrationLog savedLog = repository.save(integrationLog);

        return mapper.toIntegrationLogResponse(savedLog);
    }

    @Override
    @Transactional(readOnly = true)
    public IntegrationLogResponse getIntegrationLog(Long logId) {
        IntegrationLog log = repository.findById(logId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Integration log with id: " + logId + " was not found"
                        )
                );

        return mapper.toIntegrationLogResponse(log);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IntegrationLogResponse> getAllIntegrationLogs() {
        return repository.findAll()
                .stream()
                .map(mapper::toIntegrationLogResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<IntegrationLogResponse> getIntegrationLogsPage(int page, int size) {
        return repository.findAll(PageRequest.of(page, size))
                .map(mapper::toIntegrationLogResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<IntegrationLogResponse> searchIntegrationLogs(
            String keyword, TypeLog type, StatutLog statut, int page, int size
    ) {
        String normalized = keyword == null ? "" : keyword.trim();
        return repository.search(normalized, type, statut, PageRequest.of(page, size))
                .map(mapper::toIntegrationLogResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IntegrationLogResponse> getIntegrationLogsByType(TypeLog type) {
        return repository.findByType(type)
                .stream()
                .map(mapper::toIntegrationLogResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<IntegrationLogResponse> getIntegrationLogsByStatut(StatutLog statut) {
        return repository.findByStatut(statut)
                .stream()
                .map(mapper::toIntegrationLogResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<IntegrationLogResponse> getIntegrationLogsByCodingChallenge(Long codingChallengeId) {
        return repository.findByCodingChallengeId(codingChallengeId)
                .stream()
                .map(mapper::toIntegrationLogResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<IntegrationLogResponse> getIntegrationLogsBetweenDates(
            ZonedDateTime startDate,
            ZonedDateTime endDate
    ) {
        return repository.findByDateBetween(startDate, endDate)
                .stream()
                .map(mapper::toIntegrationLogResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public IntegrationLogResponse getLastIntegrationLog() {
        IntegrationLog log = repository.findFirstByOrderByDateDesc()
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "No integration logs found"
                        )
                );

        return mapper.toIntegrationLogResponse(log);
    }

    @Override
    @Transactional(readOnly = true)
    public long countIntegrationLogs() {
        return repository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public long countIntegrationLogsByType(TypeLog type) {
        return repository.countByType(type);
    }

    @Override
    @Transactional(readOnly = true)
    public long countIntegrationLogsByStatut(StatutLog statut) {
        return repository.countByStatut(statut);
    }

    @Override
    @Transactional(readOnly = true)
    public long countFailedIntegrations() {
        return repository.countByStatut(StatutLog.ERREUR);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long logId) {
        return repository.existsById(logId);
    }

    @Override
    public void logSyncEvent(Long codingChallengeId, StatutLog statut, String message) {
        IntegrationLog integrationLog = new IntegrationLog();
        integrationLog.setType(TypeLog.SYNC_CHALLENGE);
        integrationLog.setStatut(statut);
        integrationLog.setMessage(message);
        integrationLog.setDate(ZonedDateTime.now());

        if (codingChallengeId != null) {
            integrationLog.setCodingChallenge(
                    manager.getReference(CodingChallenge.class, codingChallengeId)
            );
        }

        repository.save(integrationLog);
    }

    @Override
    public void logEvent(TypeLog type, StatutLog statut, String message, Long codingChallengeId) {
        IntegrationLog integrationLog = new IntegrationLog();
        integrationLog.setType(type);
        integrationLog.setStatut(statut);
        integrationLog.setMessage(message);
        integrationLog.setDate(ZonedDateTime.now());

        if (codingChallengeId != null) {
            integrationLog.setCodingChallenge(
                    manager.getReference(CodingChallenge.class, codingChallengeId)
            );
        }

        repository.save(integrationLog);
        log.info("Integration log [{}][{}]: {}", type, statut, message);
    }
}