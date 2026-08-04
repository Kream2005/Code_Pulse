package com.stage.backend.repository;

import com.stage.backend.entity.Feedback;
import com.stage.backend.enums.StatutFeedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findByNoteGlobale(Float noteGlobale);
    List<Feedback> findByCommentaireContainingIgnoreCase(String commentaire);
    List<Feedback> findByStatutFeedback(StatutFeedback statutFeedback);
    List<Feedback> findByCreatedAt(ZonedDateTime createdAt);
    List<Feedback> findByUtilisateurId(Long userId);
    Page<Feedback> findByUtilisateurId(Long userId, Pageable pageable);
    @EntityGraph(attributePaths = {"utilisateur", "codingChallenge"})
    Page<Feedback> findByUtilisateurIdAndSupprimeFalse(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"utilisateur", "codingChallenge"})
    Page<Feedback> findBySupprimeFalse(Pageable pageable);
    long countByStatutFeedback(StatutFeedback statut);

    long countBySupprimeFalse();

    long countByStatutFeedbackAndSupprimeFalse(StatutFeedback statut);

    long countByUtilisateurIdAndSupprimeFalse(Long utilisateurId);

    boolean existsByUtilisateurIdAndCodingChallengeId(Long utilisateurId, Long codingChallengeId);
    boolean existsByCodingChallengeId(Long codingChallengeId);
    Optional<Feedback> findByCodingChallengeId(Long codingChallengeId);

    @Query("SELECT AVG(f.noteGlobale) FROM feedback f")
    Float getAverageNoteGlobale();
}
