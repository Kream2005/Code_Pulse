package com.stage.backend.repository;

import com.stage.backend.entity.Feedback;
import com.stage.backend.entity.IntegrationLog;
import com.stage.backend.enums.StatutLog;
import com.stage.backend.enums.TypeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IntegrationLogRepository extends JpaRepository<IntegrationLog, Long> {
    List<IntegrationLog> findByType(TypeLog type);
    List<IntegrationLog> findByStatut(StatutLog statut);
    List<IntegrationLog> findByCodingChallengeId(Long codingChallengeId);
    List<IntegrationLog> findByDateBetween(ZonedDateTime startDate, ZonedDateTime endDate);
    Optional<IntegrationLog> findFirstByOrderByDateDesc();
    long countByType(TypeLog type);
    long countByStatut(StatutLog statut);
}
