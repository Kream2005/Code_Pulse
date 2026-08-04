package com.stage.backend.repository;

import com.stage.backend.entity.CodingChallenge;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CodingChallengeRepository extends JpaRepository<CodingChallenge, Long> {
    List<CodingChallenge> findByTitreContainingIgnoreCase(String titre);
    List<CodingChallenge> findByDescriptionContainingIgnoreCase(String description);
    List<CodingChallenge> findByDuree(Integer duree);
    List<CodingChallenge> findByDateCompletion(ZonedDateTime dateCompletion);
    Optional<CodingChallenge> findByExternalId(Long id);

    Page<CodingChallenge> findBySupprimeFalse(Pageable pageable);

    List<CodingChallenge> findBySupprimeFalse();

    long countBySupprimeFalse();

    long countBySupprimeTrue();
}
