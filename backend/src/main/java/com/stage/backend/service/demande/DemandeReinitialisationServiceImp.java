package com.stage.backend.service.demande;

import com.stage.backend.config.notification.NotificationProperties;
import com.stage.backend.dto.demande.DemandeMotDePasseResponse;
import com.stage.backend.dto.demande.DemandeReinitialisationDto;
import com.stage.backend.dto.demande.DemandeTraitementResponse;
import com.stage.backend.dto.demande.ResetInfoResponse;
import com.stage.backend.dto.login.LoginResponse;
import com.stage.backend.email.NotificationEmailSender;
import com.stage.backend.entity.DemandeReinitialisation;
import com.stage.backend.entity.Utilisateur;
import com.stage.backend.enums.ResultatDemandeMotDePasse;
import com.stage.backend.enums.StatutDemandeReinit;
import com.stage.backend.enums.StatutLog;
import com.stage.backend.enums.TypeLog;
import com.stage.backend.exception.ErreurAuthentificationException;
import com.stage.backend.exception.ErreurMetierException;
import com.stage.backend.repository.DemandeReinitialisationRepository;
import com.stage.backend.repository.UtilisateurRepository;
import com.stage.backend.security.JwtProperties;
import com.stage.backend.security.JwtUtils;
import com.stage.backend.service.integrationlog.IntegrationLogService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DemandeReinitialisationServiceImp implements DemandeReinitialisationService {

    private static final int RESET_TOKEN_HOURS = 24;

    private final DemandeReinitialisationRepository demandeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationEmailSender emailSender;
    private final NotificationProperties notificationProperties;
    private final IntegrationLogService integrationLogService;
    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;

    @Override
    public DemandeMotDePasseResponse soumettreDemande(String email) {
        String normalized = email.trim();
        String emailMasque = masquerEmail(normalized);
        Utilisateur user = utilisateurRepository.findByEmail(normalized).orElse(null);

        if (user == null) {
            integrationLogService.logEvent(
                    TypeLog.AUTH,
                    StatutLog.INFO,
                    "Demande mot de passe ignorée (compte inconnu) : " + normalized,
                    null
            );
            return new DemandeMotDePasseResponse(
                    true,
                    ResultatDemandeMotDePasse.IGNORE_COMPTE_INCONNU,
                    emailMasque,
                    "Aucun compte complet trouvé pour cet e-mail — demande ignorée"
            );
        }

        if (!user.isCompteComplet() || user.getPassword() == null) {
            integrationLogService.logEvent(
                    TypeLog.AUTH,
                    StatutLog.INFO,
                    "Demande mot de passe ignorée (compte incomplet) : " + normalized,
                    null
            );
            return new DemandeMotDePasseResponse(
                    true,
                    ResultatDemandeMotDePasse.IGNORE_COMPTE_INCOMPLET,
                    emailMasque,
                    "Compte incomplet — l'utilisateur doit d'abord finaliser son inscription"
            );
        }

        if (demandeRepository.existsByEmailIgnoreCaseAndStatut(user.getEmail(), StatutDemandeReinit.EN_ATTENTE)) {
            integrationLogService.logEvent(
                    TypeLog.AUTH,
                    StatutLog.INFO,
                    "Demande mot de passe déjà en attente : " + user.getEmail(),
                    null
            );
            return new DemandeMotDePasseResponse(
                    true,
                    ResultatDemandeMotDePasse.DEJA_EN_ATTENTE,
                    emailMasque,
                    "Une demande est déjà en attente pour cet e-mail"
            );
        }

        DemandeReinitialisation demande = new DemandeReinitialisation();
        demande.setEmail(user.getEmail());
        demande.setUtilisateur(user);
        demande.setStatut(StatutDemandeReinit.EN_ATTENTE);
        demande.setDateDemande(ZonedDateTime.now());
        demandeRepository.save(demande);

        integrationLogService.logEvent(
                TypeLog.AUTH,
                StatutLog.SUCCES,
                "Demande de réinitialisation créée : " + user.getEmail(),
                null
        );
        return new DemandeMotDePasseResponse(
                true,
                ResultatDemandeMotDePasse.CREE,
                emailMasque,
                "Demande enregistrée — un administrateur la traitera prochainement"
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<DemandeReinitialisationDto> lister(StatutDemandeReinit statut) {
        List<DemandeReinitialisation> demandes = statut == null
                ? demandeRepository.findAllByOrderByDateDemandeDesc()
                : demandeRepository.findByStatutOrderByDateDemandeDesc(statut);
        return demandes.stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DemandeReinitialisationDto> listerPage(StatutDemandeReinit statut, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<DemandeReinitialisation> demandes = statut == null
                ? demandeRepository.findAllByOrderByDateDemandeDesc(pageable)
                : demandeRepository.findByStatutOrderByDateDemandeDesc(statut, pageable);
        return demandes.map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DemandeReinitialisationDto> rechercherPage(String keyword, StatutDemandeReinit statut, int page, int size) {
        String normalized = keyword == null ? "" : keyword.trim();
        return demandeRepository.search(normalized, statut, PageRequest.of(page, size)).map(this::toDto);
    }

    @Override
    public DemandeTraitementResponse envoyerLien(Long demandeId) {
        DemandeReinitialisation demande = getPending(demandeId);
        Utilisateur user = requireUser(demande);
        StatutDemandeReinit statutPrecedent = demande.getStatut();

        String token = UUID.randomUUID().toString();
        demande.setResetToken(token);
        demande.setResetTokenExpiresAt(ZonedDateTime.now().plusHours(RESET_TOKEN_HOURS));
        demande.setStatut(StatutDemandeReinit.LIEN_ENVOYE);
        demande.setDateTraitement(ZonedDateTime.now());
        demande.setTraitePar(currentAdmin());
        demandeRepository.save(demande);

        String actionUrl = notificationProperties.frontendBaseUrl() + "/reset-password?token=" + token;
        boolean emailEnvoye;
        String message;
        try {
            emailSender.sendPasswordResetLink(user, actionUrl);
            emailEnvoye = true;
            message = "Lien de réinitialisation envoyé à " + user.getEmail();
            integrationLogService.logEvent(
                    TypeLog.AUTH,
                    StatutLog.SUCCES,
                    "Lien de réinitialisation envoyé : " + user.getEmail(),
                    null
            );
        } catch (RuntimeException exception) {
            emailEnvoye = false;
            message = "Demande traitée mais échec d'envoi e-mail : " + exception.getMessage();
            integrationLogService.logEvent(
                    TypeLog.AUTH,
                    StatutLog.ERREUR,
                    "Échec envoi lien réinitialisation : " + exception.getMessage(),
                    null
            );
        }
        return new DemandeTraitementResponse(toDto(demande), emailEnvoye, actionUrl, statutPrecedent, message);
    }

    @Override
    public DemandeTraitementResponse definirMotDePasseTemporaire(Long demandeId, String temporaryPassword) {
        DemandeReinitialisation demande = getPending(demandeId);
        Utilisateur user = requireUser(demande);
        StatutDemandeReinit statutPrecedent = demande.getStatut();

        user.setPassword(passwordEncoder.encode(temporaryPassword));
        utilisateurRepository.save(user);

        demande.setStatut(StatutDemandeReinit.MOT_DE_PASSE_TEMPORAIRE);
        demande.setDateTraitement(ZonedDateTime.now());
        demande.setTraitePar(currentAdmin());
        demande.setResetToken(null);
        demande.setResetTokenExpiresAt(null);
        demandeRepository.save(demande);

        boolean emailEnvoye;
        String message;
        try {
            emailSender.sendTemporaryPasswordEmail(user, temporaryPassword);
            emailEnvoye = true;
            message = "Mot de passe temporaire défini et envoyé à " + user.getEmail();
            integrationLogService.logEvent(
                    TypeLog.AUTH,
                    StatutLog.SUCCES,
                    "Mot de passe temporaire défini : " + user.getEmail(),
                    null
            );
        } catch (RuntimeException exception) {
            emailEnvoye = false;
            message = "Mot de passe temporaire défini mais échec d'envoi e-mail : " + exception.getMessage();
            integrationLogService.logEvent(
                    TypeLog.AUTH,
                    StatutLog.ERREUR,
                    "Échec envoi mot de passe temporaire : " + exception.getMessage(),
                    null
            );
        }
        return new DemandeTraitementResponse(toDto(demande), emailEnvoye, null, statutPrecedent, message);
    }

    @Override
    public DemandeTraitementResponse rejeter(Long demandeId) {
        DemandeReinitialisation demande = getPending(demandeId);
        StatutDemandeReinit statutPrecedent = demande.getStatut();
        demande.setStatut(StatutDemandeReinit.REJETEE);
        demande.setDateTraitement(ZonedDateTime.now());
        demande.setTraitePar(currentAdmin());
        demandeRepository.save(demande);

        integrationLogService.logEvent(
                TypeLog.AUTH,
                StatutLog.WARNING,
                "Demande de réinitialisation rejetée : " + demande.getEmail(),
                null
        );
        return new DemandeTraitementResponse(
                toDto(demande),
                false,
                null,
                statutPrecedent,
                "Demande rejetée pour " + demande.getEmail()
        );
    }

    @Override
    public LoginResponse reinitialiserMotDePasse(String token, String password) {
        DemandeReinitialisation demande = demandeRepository.findByResetToken(token)
                .orElseThrow(() -> new ErreurAuthentificationException(
                        "JETON_INVALIDE",
                        "Jeton de réinitialisation invalide"
                ));

        if (demande.getStatut() != StatutDemandeReinit.LIEN_ENVOYE) {
            throw new ErreurMetierException(
                    HttpStatus.BAD_REQUEST,
                    "LIEN_INVALIDE",
                    "Ce lien de réinitialisation n'est plus valide"
            );
        }
        if (demande.getResetTokenExpiresAt() == null
                || demande.getResetTokenExpiresAt().isBefore(ZonedDateTime.now())) {
            throw new ErreurMetierException(
                    HttpStatus.BAD_REQUEST,
                    "JETON_EXPIRE",
                    "Le jeton de réinitialisation a expiré"
            );
        }

        Utilisateur user = requireUser(demande);
        user.setPassword(passwordEncoder.encode(password));
        utilisateurRepository.save(user);

        demande.setResetToken(null);
        demande.setResetTokenExpiresAt(null);
        demandeRepository.save(demande);

        integrationLogService.logEvent(
                TypeLog.AUTH,
                StatutLog.SUCCES,
                "Password reset completed: " + user.getEmail(),
                null
        );

        return issueToken(user);
    }

    @Override
    @Transactional(readOnly = true)
    public ResetInfoResponse getResetInfo(String token) {
        DemandeReinitialisation demande = demandeRepository.findByResetToken(token).orElse(null);
        if (demande == null) {
            return new ResetInfoResponse(
                    false,
                    null,
                    null,
                    "Jeton de réinitialisation invalide"
            );
        }
        if (demande.getStatut() != StatutDemandeReinit.LIEN_ENVOYE) {
            return new ResetInfoResponse(
                    false,
                    demande.getEmail(),
                    demande.getResetTokenExpiresAt(),
                    "Ce lien de réinitialisation n'est plus valide (statut : " + demande.getStatut() + ")"
            );
        }
        if (demande.getResetTokenExpiresAt() == null
                || demande.getResetTokenExpiresAt().isBefore(ZonedDateTime.now())) {
            return new ResetInfoResponse(
                    false,
                    demande.getEmail(),
                    demande.getResetTokenExpiresAt(),
                    "Le jeton de réinitialisation a expiré"
            );
        }
        return new ResetInfoResponse(
                true,
                demande.getEmail(),
                demande.getResetTokenExpiresAt(),
                "Jeton valide — vous pouvez définir un nouveau mot de passe"
        );
    }

    private DemandeReinitialisation getPending(Long demandeId) {
        DemandeReinitialisation demande = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Demande de réinitialisation introuvable : id=" + demandeId
                ));
        if (demande.getStatut() != StatutDemandeReinit.EN_ATTENTE) {
            throw new ErreurMetierException(
                    HttpStatus.BAD_REQUEST,
                    "DEMANDE_DEJA_TRAITEE",
                    "Cette demande a déjà été traitée (statut : " + demande.getStatut() + ")"
            );
        }
        return demande;
    }

    private Utilisateur requireUser(DemandeReinitialisation demande) {
        Utilisateur user = demande.getUtilisateur();
        if (user == null) {
            throw new ErreurMetierException(
                    HttpStatus.BAD_REQUEST,
                    "UTILISATEUR_LIE_MANQUANT",
                    "Aucun utilisateur lié à cette demande"
            );
        }
        return user;
    }

    private static String masquerEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        int at = email.indexOf('@');
        if (at <= 1) {
            return "*" + email.substring(at);
        }
        return email.charAt(0) + "***" + email.substring(at);
    }

    private Utilisateur currentAdmin() {
        Long adminId = JwtUtils.getCurrentUserId();
        return utilisateurRepository.findById(adminId)
                .orElseThrow(() -> new EntityNotFoundException("Admin not found: " + adminId));
    }

    private LoginResponse issueToken(Utilisateur user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(jwtProperties.accessTokenTtl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .subject(user.getEmail())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim("roles", List.of(user.getRole().name()))
                .claim("uid", user.getId())
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        String tokenValue = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new LoginResponse(tokenValue, "Bearer", jwtProperties.accessTokenTtl());
    }

    private DemandeReinitialisationDto toDto(DemandeReinitialisation demande) {
        Utilisateur user = demande.getUtilisateur();
        Utilisateur admin = demande.getTraitePar();
        return new DemandeReinitialisationDto(
                demande.getId(),
                demande.getEmail(),
                user != null ? user.getId() : null,
                user != null ? user.getNom() : null,
                user != null ? user.getPrenom() : null,
                demande.getStatut(),
                demande.getDateDemande(),
                demande.getDateTraitement(),
                admin != null ? admin.getId() : null,
                admin != null ? admin.getEmail() : null
        );
    }
}
