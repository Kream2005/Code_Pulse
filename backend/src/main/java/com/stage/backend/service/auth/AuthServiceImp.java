package com.stage.backend.service.auth;

import com.stage.backend.dto.login.LoginResponse;
import com.stage.backend.dto.utilisateur.CompleteAccountRequest;
import com.stage.backend.dto.utilisateur.SetupAccountInfoResponse;
import com.stage.backend.entity.Utilisateur;
import com.stage.backend.enums.StatutLog;
import com.stage.backend.enums.TypeLog;
import com.stage.backend.repository.UtilisateurRepository;
import com.stage.backend.security.JwtProperties;
import com.stage.backend.service.integrationlog.IntegrationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthServiceImp implements AuthService {

    private final UtilisateurRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;
    private final IntegrationLogService integrationLogService;

    @Override
    public LoginResponse login(String email, String password) {
        Utilisateur utilisateur = repository.findByEmail(email)
                .orElseThrow(() -> {
                    integrationLogService.logEvent(
                            TypeLog.AUTH,
                            StatutLog.ERREUR,
                            "Login failed for unknown email: " + email,
                            null
                    );
                    return new BadCredentialsException("Invalid Credentials");
                });

        if (utilisateur.isSupprime()) {
            integrationLogService.logEvent(
                    TypeLog.AUTH,
                    StatutLog.ERREUR,
                    "Login failed — account archived: " + email,
                    null
            );
            throw new BadCredentialsException("Account is no longer active");
        }

        if (!utilisateur.isCompteComplet() || utilisateur.getPassword() == null) {
            integrationLogService.logEvent(
                    TypeLog.AUTH,
                    StatutLog.ERREUR,
                    "Login failed — account setup incomplete: " + email,
                    null
            );
            throw new BadCredentialsException("Account setup not completed");
        }

        if (!passwordEncoder.matches(password, utilisateur.getPassword())) {
            integrationLogService.logEvent(
                    TypeLog.AUTH,
                    StatutLog.ERREUR,
                    "Login failed — invalid password: " + email,
                    null
            );
            throw new BadCredentialsException("Invalid Password");
        }

        integrationLogService.logEvent(
                TypeLog.AUTH,
                StatutLog.SUCCES,
                "Login successful: " + email,
                null
        );

        return issueToken(utilisateur);
    }

    @Override
    public LoginResponse completeAccount(CompleteAccountRequest request) {
        Utilisateur user = repository.findBySetupToken(request.token())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid setup token"));

        if (user.isCompteComplet()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account already completed");
        }
        if (user.getSetupTokenExpiresAt() == null
                || user.getSetupTokenExpiresAt().isBefore(ZonedDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Setup token expired");
        }

        applyMissingProfile(user, request);

        if (!StringUtils.hasText(user.getNom()) || !StringUtils.hasText(user.getPrenom())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Last name and first name are required to complete the account"
            );
        }

        user.setPassword(passwordEncoder.encode(request.password()));
        user.setCompteComplet(true);
        user.setStatus(true);
        user.setSetupToken(null);
        user.setSetupTokenExpiresAt(null);
        repository.save(user);

        integrationLogService.logEvent(
                TypeLog.AUTH,
                StatutLog.SUCCES,
                "Account completed: " + user.getEmail(),
                null
        );

        return issueToken(user);
    }

    @Override
    @Transactional(readOnly = true)
    public SetupAccountInfoResponse getSetupAccountInfo(String token) {
        Utilisateur user = repository.findBySetupToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid setup token"));

        if (user.isCompteComplet()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account already completed");
        }
        if (user.getSetupTokenExpiresAt() == null
                || user.getSetupTokenExpiresAt().isBefore(ZonedDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Setup token expired");
        }

        return new SetupAccountInfoResponse(
                user.getEmail(),
                user.getNom(),
                user.getPrenom(),
                user.getUserName()
        );
    }

    private void applyMissingProfile(Utilisateur user, CompleteAccountRequest request) {
        if (!StringUtils.hasText(user.getNom()) && StringUtils.hasText(request.nom())) {
            user.setNom(request.nom().trim());
        }
        if (!StringUtils.hasText(user.getPrenom()) && StringUtils.hasText(request.prenom())) {
            user.setPrenom(request.prenom().trim());
        }
        if (!StringUtils.hasText(user.getUserName()) && StringUtils.hasText(request.userName())) {
            String candidate = request.userName().trim();
            if (repository.existsByUserName(candidate)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username already in use");
            }
            user.setUserName(candidate);
        }
    }

    private LoginResponse issueToken(Utilisateur utilisateur) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(jwtProperties.accessTokenTtl());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .subject(utilisateur.getEmail())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim("roles", List.of(utilisateur.getRole().name()))
                .claim("uid", utilisateur.getId())
                .build();

        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        String tokenValue = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new LoginResponse(tokenValue, "Bearer", jwtProperties.accessTokenTtl());
    }
}
