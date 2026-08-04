package com.stage.backend.service.utilisateur;

import com.stage.backend.dto.profile.ProfileDto;
import com.stage.backend.dto.utilisateur.CreateUtilisateurRequest;
import com.stage.backend.dto.utilisateur.UpdateUtilisateurRequest;
import com.stage.backend.dto.utilisateur.UtilisateurDto;
import com.stage.backend.enums.Role;
import org.springframework.data.domain.Page;

import java.time.ZonedDateTime;
import java.util.List;

public interface UtilisateurService {
    UtilisateurDto registerUtilisateur(CreateUtilisateurRequest request);

    UtilisateurDto modifierUtilisateur(UpdateUtilisateurRequest request, Long userId);

    boolean supprimerUtilisateur(Long userId);

    UtilisateurDto getUtilisateur(Long userId);

    List<UtilisateurDto> getAllUtilisateurs();

    List<UtilisateurDto> getUtilisateursByRole(Role role);

    UtilisateurDto getUtilisateurByEmail(String email);

    UtilisateurDto changePassword(Long userId, String oldPassword, String newPassword);

    // Utilisation de pagination pour optimiser le chargement des données
    Page<UtilisateurDto> getUtilisateursPage(int page, int size);

    long countUtilisateursByRole(Role role);

    long countTotalUtilisateurs();

    void updateLastLogin(Long userId);

    long getDaysSinceLastLogin(Long userId);

    UtilisateurDto resetPassword(String email);

    void updateHoursLoggedIn(Long userId);

    boolean existsByEmail(String email);

    UtilisateurDto promoteRole(Long userId, Role role);

    ZonedDateTime getLastLogin(Long userId);
}
