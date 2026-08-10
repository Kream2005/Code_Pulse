package com.stage.backend.service.utilisateur;

import com.stage.backend.dto.utilisateur.CreateUtilisateurRequest;
import com.stage.backend.dto.utilisateur.UpdateUtilisateurRequest;
import com.stage.backend.dto.utilisateur.UtilisateurDto;
import com.stage.backend.entity.Utilisateur;
import com.stage.backend.enums.Role;
import com.stage.backend.enums.StatutLog;
import com.stage.backend.enums.TypeLog;
import com.stage.backend.mapper.UtilisateurMapper;
import com.stage.backend.repository.UtilisateurRepository;
import com.stage.backend.service.integrationlog.IntegrationLogService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZonedDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UtilisateurServiceImp implements UtilisateurService{

    private final UtilisateurRepository repository;
    private final UtilisateurMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final IntegrationLogService integrationLogService;

    @Override
    public UtilisateurDto registerUtilisateur(CreateUtilisateurRequest request) {
        log.info("adding user with email: '{}'", request.email());

        if (request.role() == Role.USER) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Collaborateurs are provisioned from coding-challenge events, not created manually"
            );
        }
        if (request.role() != Role.MANAGER_RH
                && request.role() != Role.ADMIN_CODING_CHALLENGE
                && request.role() != Role.ADMIN_CODEPULSE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported role: " + request.role());
        }
        if (repository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already in use");
        }

        Utilisateur user = new Utilisateur();

        user.setNom(request.nom());
        user.setPrenom(request.prenom());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.rawPassword()));
        user.setRole(request.role());
        user.setCompteComplet(true);
        user.setStatus(true);

        Utilisateur saved = repository.save(user);
        integrationLogService.logEvent(
                TypeLog.GESTION_UTILISATEUR,
                StatutLog.SUCCES,
                "User created by admin: " + saved.getEmail(),
                null
        );

        return mapper.toUtilisateurDto(saved);
    }

    @Override
    public UtilisateurDto modifierUtilisateur(UpdateUtilisateurRequest request, Long userId) {
        Utilisateur user = repository.findById(userId).orElseThrow(
                () -> new EntityNotFoundException(
                        "User with id: " + userId + "was not found"
                )
        );

        user.setNom(request.nom());
        user.setPrenom(request.prenom());
        user.setEmail(request.email());
        user.setRole(request.role());

        Utilisateur updated = repository.save(user);
        integrationLogService.logEvent(
                TypeLog.GESTION_UTILISATEUR,
                StatutLog.SUCCES,
                "User updated: " + updated.getEmail(),
                null
        );
        return mapper.toUtilisateurDto(updated);
    }

    @Override
    public boolean supprimerUtilisateur(Long userId) {
        return repository.findById(userId).map(user -> {
            user.setSupprime(true);
            user.setStatus(false);
            repository.save(user);
            integrationLogService.logEvent(
                    TypeLog.GESTION_UTILISATEUR,
                    StatutLog.SUCCES,
                    "User soft-deleted (feedbacks kept): " + user.getEmail(),
                    null
            );
            return true;
        }).orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public UtilisateurDto getUtilisateur(Long userId) {
        Utilisateur user = repository.findById(userId).orElseThrow(
                () -> new EntityNotFoundException(
                        "User with id: " + userId + "was not found"
                )
        );

        return mapper.toUtilisateurDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UtilisateurDto> getAllUtilisateurs() {
        return repository.findBySupprimeFalse()
                .stream()
                .map(mapper::toUtilisateurDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UtilisateurDto> getUtilisateursByRole(Role role) {
        return repository.findByRole(role)
                .stream()
                .map(mapper::toUtilisateurDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UtilisateurDto getUtilisateurByEmail(String email) {
        Utilisateur user = repository.findByEmail(email).orElseThrow(
                () -> new EntityNotFoundException(
                        "User with email: " + email + "was not found"
                )
        );

        return mapper.toUtilisateurDto(user);
    }

    @Override
    public boolean existsByEmail(String email) {
        return repository.existsByEmail(email);
    }

    @Override
    public UtilisateurDto promoteRole(Long userId, Role role) {
        Utilisateur user = repository.findById(userId).orElseThrow(
                () -> new EntityNotFoundException(
                        "User with id: " + userId + " was not found"
                )
        );
        Role previousRole = user.getRole();
        user.setRole(role);
        Utilisateur saved = repository.save(user);
        integrationLogService.logEvent(
                TypeLog.GESTION_UTILISATEUR,
                StatutLog.SUCCES,
                "Role changed for " + saved.getEmail() + ": " + previousRole + " -> " + role,
                null
        );
        return mapper.toUtilisateurDto(saved);
    }

    @Override
    public UtilisateurDto changePassword(Long userId, String oldPassword, String newPassword) {
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UtilisateurDto> getUtilisateursPage(int page, int size) {
        return repository.findBySupprimeFalse(PageRequest.of(page, size))
                .map(mapper::toUtilisateurDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UtilisateurDto> searchUtilisateurs(String keyword, Role role, int page, int size) {
        String normalized = keyword == null ? "" : keyword.trim();
        return repository.search(normalized, role, PageRequest.of(page, size))
                .map(mapper::toUtilisateurDto);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUtilisateursByRole(Role role) {
        return repository.countByRole(role);
    }

    @Override
    @Transactional(readOnly = true)
    public long countTotalUtilisateurs() {
        return repository.count();
    }

    @Override
    public void updateLastLogin(Long userId) {

    }

    @Override
    public long getDaysSinceLastLogin(Long userId) {
        return 0;
    }

    @Override
    public UtilisateurDto resetPassword(String email) {
        return null;
    }

    @Override
    public void updateHoursLoggedIn(Long userId) {

    }

    @Override
    public ZonedDateTime getLastLogin(Long userId) {
        return null;
    }
}
