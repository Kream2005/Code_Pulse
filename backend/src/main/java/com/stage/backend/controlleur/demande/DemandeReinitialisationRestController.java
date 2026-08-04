package com.stage.backend.controlleur.demande;

import com.stage.backend.dto.demande.DemandeReinitialisationDto;
import com.stage.backend.dto.demande.SetTemporaryPasswordRequest;
import com.stage.backend.enums.StatutDemandeReinit;
import com.stage.backend.security.SecurityRoles;
import com.stage.backend.service.demande.DemandeReinitialisationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/demandes-reinit")
@RequiredArgsConstructor
public class DemandeReinitialisationRestController {

    private final DemandeReinitialisationService service;

    @GetMapping
    @PreAuthorize(SecurityRoles.ADMIN_CODEPULSE)
    public ResponseEntity<List<DemandeReinitialisationDto>> lister(
            @RequestParam(required = false) StatutDemandeReinit statut
    ) {
        return ResponseEntity.ok(service.lister(statut));
    }

    @GetMapping("/page")
    @PreAuthorize(SecurityRoles.ADMIN_CODEPULSE)
    public ResponseEntity<Page<DemandeReinitialisationDto>> listerPage(
            @RequestParam(required = false) StatutDemandeReinit statut,
            @RequestParam(required = false) String q,
            @RequestParam int page,
            @RequestParam int size
    ) {
        return ResponseEntity.ok(service.rechercherPage(q, statut, page, size));
    }

    @PostMapping("/{id}/send-link")
    @PreAuthorize(SecurityRoles.ADMIN_CODEPULSE)
    public ResponseEntity<DemandeReinitialisationDto> envoyerLien(@PathVariable Long id) {
        return ResponseEntity.ok(service.envoyerLien(id));
    }

    @PostMapping("/{id}/temporary-password")
    @PreAuthorize(SecurityRoles.ADMIN_CODEPULSE)
    public ResponseEntity<DemandeReinitialisationDto> temporaryPassword(
            @PathVariable Long id,
            @Valid @RequestBody SetTemporaryPasswordRequest request
    ) {
        return ResponseEntity.ok(service.definirMotDePasseTemporaire(id, request.temporaryPassword()));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize(SecurityRoles.ADMIN_CODEPULSE)
    public ResponseEntity<DemandeReinitialisationDto> rejeter(@PathVariable Long id) {
        return ResponseEntity.ok(service.rejeter(id));
    }
}
