package com.stage.backend.repository;

import com.stage.backend.entity.CodingChallenge;
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

    @Query("""
            SELECT c FROM coding_challenge c
            WHERE c.supprime = false
            AND (:tag IS NULL OR :tag = '' OR LOWER(c.tag) = LOWER(:tag))
            AND (
                :keyword IS NULL OR :keyword = ''
                OR LOWER(c.titre) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(c.tag) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            """)
    Page<CodingChallenge> search(@Param("keyword") String keyword, @Param("tag") String tag, Pageable pageable);

    @Query("""
            SELECT DISTINCT c.tag FROM coding_challenge c
            WHERE c.supprime = false
            AND c.tag IS NOT NULL
            AND c.tag <> ''
            ORDER BY c.tag
            """)
    List<String> findDistinctTags();
}
