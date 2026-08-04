package com.stage.backend.service.demande;

import com.stage.backend.dto.demande.DemandeReinitialisationDto;
import com.stage.backend.dto.login.LoginResponse;
import com.stage.backend.enums.StatutDemandeReinit;
import org.springframework.data.domain.Page;

import java.util.List;

public interface DemandeReinitialisationService {

    void soumettreDemande(String email);

    List<DemandeReinitialisationDto> lister(StatutDemandeReinit statut);

    Page<DemandeReinitialisationDto> listerPage(StatutDemandeReinit statut, int page, int size);

    DemandeReinitialisationDto envoyerLien(Long demandeId);

    DemandeReinitialisationDto definirMotDePasseTemporaire(Long demandeId, String temporaryPassword);

    DemandeReinitialisationDto rejeter(Long demandeId);

    LoginResponse reinitialiserMotDePasse(String token, String password);

    String getEmailByResetToken(String token);
}
