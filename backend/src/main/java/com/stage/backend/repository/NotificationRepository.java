package com.stage.backend.repository;

import com.stage.backend.entity.Notification;
import com.stage.backend.enums.StatutNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @EntityGraph(attributePaths = {"codingChallenge", "utilisateur"})
    List<Notification> findByUtilisateurId(Long utilisateurId);

    @EntityGraph(attributePaths = {"codingChallenge", "utilisateur"})
    Page<Notification> findByUtilisateurIdAndSupprimeFalse(Long utilisateurId, Pageable pageable);

    @EntityGraph(attributePaths = {"codingChallenge", "utilisateur"})
    Page<Notification> findByUtilisateurId(Long utilisateurId, Pageable pageable);

    List<Notification> findByCodingChallengeId(Long codingChallengeId);

    @EntityGraph(attributePaths = {"codingChallenge", "utilisateur"})
    List<Notification> findByStatut(StatutNotification statut);

    @EntityGraph(attributePaths = {"codingChallenge", "utilisateur"})
    Page<Notification> findByStatut(StatutNotification statut, Pageable pageable);

    @EntityGraph(attributePaths = {"codingChallenge", "utilisateur"})
    Page<Notification> findAllBy(Pageable pageable);

    long countByStatut(StatutNotification statut);

    long countBySupprimeFalse();

    long countByStatutAndSupprimeFalse(StatutNotification statut);

    long countByUtilisateurIdAndSupprimeFalse(Long utilisateurId);

    long countByUtilisateurIdAndStatutAndSupprimeFalse(Long utilisateurId, StatutNotification statut);

    Optional<Notification> findByUtilisateurIdAndCodingChallengeId(
            Long utilisateurId,
            Long codingChallengeId
    );

    @Query("""
            select distinct n from notification n
            left join fetch n.codingChallenge
            left join fetch n.utilisateur
            """)
    List<Notification> findAllWithDetails();
}
