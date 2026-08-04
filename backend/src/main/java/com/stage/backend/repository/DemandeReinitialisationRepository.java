package com.stage.backend.repository;

import com.stage.backend.entity.DemandeReinitialisation;
import com.stage.backend.enums.StatutDemandeReinit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
