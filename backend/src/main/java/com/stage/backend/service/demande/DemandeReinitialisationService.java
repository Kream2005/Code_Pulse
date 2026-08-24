package com.stage.backend.service.demande;

import com.stage.backend.dto.demande.DemandeMotDePasseResponse;
import com.stage.backend.dto.demande.DemandeReinitialisationDto;
import com.stage.backend.dto.demande.DemandeTraitementResponse;
import com.stage.backend.dto.demande.ResetInfoResponse;
import com.stage.backend.dto.login.LoginResponse;
import com.stage.backend.enums.StatutDemandeReinit;
import org.springframework.data.domain.Page;

import java.util.List;

public interface DemandeReinitialisationService {

    DemandeMotDePasseResponse soumettreDemande(String email);

    List<DemandeReinitialisationDto> lister(StatutDemandeReinit statut);

    Page<DemandeReinitialisationDto> listerPage(StatutDemandeReinit statut, int page, int size);

    Page<DemandeReinitialisationDto> rechercherPage(String keyword, StatutDemandeReinit statut, int page, int size);

    DemandeTraitementResponse envoyerLien(Long demandeId);

    DemandeTraitementResponse definirMotDePasseTemporaire(Long demandeId, String temporaryPassword);

    DemandeTraitementResponse rejeter(Long demandeId);

    LoginResponse reinitialiserMotDePasse(String token, String password);

    ResetInfoResponse getResetInfo(String token);
}
