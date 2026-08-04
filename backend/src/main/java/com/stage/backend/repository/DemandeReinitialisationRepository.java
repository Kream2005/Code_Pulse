package com.stage.backend.repository;

import com.stage.backend.entity.DemandeReinitialisation;
import com.stage.backend.enums.StatutDemandeReinit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DemandeReinitialisationRepository extends JpaRepository<DemandeReinitialisation, Long> {

    List<DemandeReinitialisation> findByStatutOrderByDateDemandeDesc(StatutDemandeReinit statut);

    Page<DemandeReinitialisation> findByStatutOrderByDateDemandeDesc(StatutDemandeReinit statut, Pageable pageable);

    List<DemandeReinitialisation> findAllByOrderByDateDemandeDesc();

    Page<DemandeReinitialisation> findAllByOrderByDateDemandeDesc(Pageable pageable);

    Optional<DemandeReinitialisation> findByResetToken(String resetToken);

    boolean existsByEmailIgnoreCaseAndStatut(String email, StatutDemandeReinit statut);

    long countByStatut(StatutDemandeReinit statut);

    @Query("""
            SELECT d FROM demande_reinitialisation d
            LEFT JOIN d.utilisateur u
            WHERE (:statut IS NULL OR d.statut = :statut)
            AND (
                :keyword IS NULL OR :keyword = ''
                OR LOWER(d.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(u.nom) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(u.prenom) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            ORDER BY d.dateDemande DESC
            """)
    Page<DemandeReinitialisation> search(
            @Param("keyword") String keyword,
            @Param("statut") StatutDemandeReinit statut,
            Pageable pageable
    );
}
