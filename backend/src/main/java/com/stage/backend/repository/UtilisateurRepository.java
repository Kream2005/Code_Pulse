package com.stage.backend.repository;

import com.stage.backend.entity.Utilisateur;
import com.stage.backend.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {
    Optional<Utilisateur> findByEmail(String email);
    Optional<Utilisateur> findByExternalId(Long externalId);
    Optional<Utilisateur> findByUserName(String userName);
    List<Utilisateur> findByRole(Role role);
    long countByRole(Role role);
    boolean existsByEmail(String email);
    boolean existsByUserName(String userName);
    Optional<Utilisateur> findBySetupToken(String setupToken);

    Page<Utilisateur> findBySupprimeFalse(Pageable pageable);

    List<Utilisateur> findBySupprimeFalse();

    long countBySupprimeFalse();

    long countByRoleAndSupprimeFalse(Role role);

    @Query("""
            SELECT u FROM Utilisateur u
            WHERE u.supprime = false
            AND (:role IS NULL OR u.role = :role)
            AND (
                :keyword IS NULL OR :keyword = ''
                OR LOWER(u.nom) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(u.prenom) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(u.userName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            """)
    Page<Utilisateur> search(@Param("keyword") String keyword, @Param("role") Role role, Pageable pageable);
}
