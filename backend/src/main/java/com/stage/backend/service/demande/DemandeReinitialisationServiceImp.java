package com.stage.backend.service.demande;

import com.stage.backend.config.notification.NotificationProperties;
import com.stage.backend.dto.demande.DemandeReinitialisationDto;
import com.stage.backend.dto.login.LoginResponse;
import com.stage.backend.email.NotificationEmailSender;
import com.stage.backend.entity.DemandeReinitialisation;
import com.stage.backend.entity.Utilisateur;
import com.stage.backend.enums.StatutDemandeReinit;
import com.stage.backend.enums.StatutLog;
import com.stage.backend.enums.TypeLog;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
    public void soumettreDemande(String email) {
        String normalized = email.trim();
        Utilisateur user = utilisateurRepository.findByEmail(normalized).orElse(null);

        if (user == null || !user.isCompteComplet() || user.getPassword() == null) {
            integrationLogService.logEvent(
                    TypeLog.AUTH,
                    StatutLog.INFO,
                    "Password reset request ignored (unknown or incomplete account): " + normalized,
                    null
            );
            return;
        }

        if (demandeRepository.existsByEmailIgnoreCaseAndStatut(user.getEmail(), StatutDemandeReinit.EN_ATTENTE)) {
            integrationLogService.logEvent(
                    TypeLog.AUTH,
                    StatutLog.INFO,
                    "Password reset request already pending: " + user.getEmail(),
                    null
            );
            return;
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
                "Password reset request submitted: " + user.getEmail(),
                null
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
    public DemandeReinitialisationDto envoyerLien(Long demandeId) {
        DemandeReinitialisation demande = getPending(demandeId);
        Utilisateur user = requireUser(demande);

        String token = UUID.randomUUID().toString();
        demande.setResetToken(token);
        demande.setResetTokenExpiresAt(ZonedDateTime.now().plusHours(RESET_TOKEN_HOURS));
        demande.setStatut(StatutDemandeReinit.LIEN_ENVOYE);
        demande.setDateTraitement(ZonedDateTime.now());
        demande.setTraitePar(currentAdmin());
        demandeRepository.save(demande);

        String actionUrl = notificationProperties.frontendBaseUrl() + "/reset-password?token=" + token;
        emailSender.sendPasswordResetLink(user, actionUrl);

        integrationLogService.logEvent(
                TypeLog.AUTH,
                StatutLog.SUCCES,
                "Password reset link sent for: " + user.getEmail(),
                null
        );
        return toDto(demande);
    }

    @Override
    public DemandeReinitialisationDto definirMotDePasseTemporaire(Long demandeId, String temporaryPassword) {
        DemandeReinitialisation demande = getPending(demandeId);
        Utilisateur user = requireUser(demande);

        user.setPassword(passwordEncoder.encode(temporaryPassword));
        utilisateurRepository.save(user);

        demande.setStatut(StatutDemandeReinit.MOT_DE_PASSE_TEMPORAIRE);
        demande.setDateTraitement(ZonedDateTime.now());
        demande.setTraitePar(currentAdmin());
        demande.setResetToken(null);
        demande.setResetTokenExpiresAt(null);
        demandeRepository.save(demande);

        emailSender.sendTemporaryPasswordEmail(user, temporaryPassword);

        integrationLogService.logEvent(
                TypeLog.AUTH,
                StatutLog.SUCCES,
                "Temporary password set for: " + user.getEmail(),
                null
        );
        return toDto(demande);
    }

    @Override
    public DemandeReinitialisationDto rejeter(Long demandeId) {
        DemandeReinitialisation demande = getPending(demandeId);
        demande.setStatut(StatutDemandeReinit.REJETEE);
        demande.setDateTraitement(ZonedDateTime.now());
        demande.setTraitePar(currentAdmin());
        demandeRepository.save(demande);

        integrationLogService.logEvent(
                TypeLog.AUTH,
                StatutLog.WARNING,
                "Password reset request rejected: " + demande.getEmail(),
                null
        );
        return toDto(demande);
    }

    @Override
    public LoginResponse reinitialiserMotDePasse(String token, String password) {
        DemandeReinitialisation demande = demandeRepository.findByResetToken(token)
                .orElseThrow(() -> new BadCredentialsException("Invalid reset token"));

        if (demande.getStatut() != StatutDemandeReinit.LIEN_ENVOYE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reset link is no longer valid");
        }
        if (demande.getResetTokenExpiresAt() == null
                || demande.getResetTokenExpiresAt().isBefore(ZonedDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reset token expired");
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
    public String getEmailByResetToken(String token) {
        DemandeReinitialisation demande = demandeRepository.findByResetToken(token)
                .orElseThrow(() -> new BadCredentialsException("Invalid reset token"));
        if (demande.getStatut() != StatutDemandeReinit.LIEN_ENVOYE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reset link is no longer valid");
        }
        if (demande.getResetTokenExpiresAt() == null
                || demande.getResetTokenExpiresAt().isBefore(ZonedDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reset token expired");
        }
        return demande.getEmail();
    }

    private DemandeReinitialisation getPending(Long demandeId) {
        DemandeReinitialisation demande = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new EntityNotFoundException("Reset request not found: " + demandeId));
        if (demande.getStatut() != StatutDemandeReinit.EN_ATTENTE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request already processed");
        }
        return demande;
    }

    private Utilisateur requireUser(DemandeReinitialisation demande) {
        Utilisateur user = demande.getUtilisateur();
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No user linked to this request");
        }
        return user;
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
