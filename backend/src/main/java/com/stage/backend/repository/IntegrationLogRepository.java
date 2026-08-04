package com.stage.backend.repository;

import com.stage.backend.entity.Feedback;
import com.stage.backend.entity.IntegrationLog;
import com.stage.backend.enums.StatutLog;
import com.stage.backend.enums.TypeLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("""
            SELECT l FROM integration_log l
            WHERE (:type IS NULL OR l.type = :type)
            AND (:statut IS NULL OR l.statut = :statut)
            AND (:keyword IS NULL OR :keyword = '' OR LOWER(l.message) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY l.date DESC
            """)
    Page<IntegrationLog> search(
            @Param("keyword") String keyword,
            @Param("type") TypeLog type,
            @Param("statut") StatutLog statut,
            Pageable pageable
    );
}
