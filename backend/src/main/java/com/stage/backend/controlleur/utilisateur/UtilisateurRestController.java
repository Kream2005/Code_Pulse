package com.stage.backend.controlleur.utilisateur;

import com.stage.backend.dto.common.SuppressionResponse;
import com.stage.backend.dto.utilisateur.CreateUtilisateurRequest;
import com.stage.backend.dto.utilisateur.PromoteRoleRequest;
import com.stage.backend.dto.utilisateur.PromoteRoleResponse;
import com.stage.backend.dto.utilisateur.UpdateUtilisateurRequest;
import com.stage.backend.dto.utilisateur.UtilisateurDto;
import com.stage.backend.enums.Role;
import com.stage.backend.security.SecurityRoles;
import com.stage.backend.service.utilisateur.UtilisateurService;
import com.stage.backend.util.PaginationUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/utilisateurs")
@RequiredArgsConstructor
@Validated
public class UtilisateurRestController {

    private final UtilisateurService service;

    @PostMapping("/add-user")
    @PreAuthorize(SecurityRoles.ADMIN_CODEPULSE)
    public ResponseEntity<UtilisateurDto> ajouterUtilisateur(
            @Valid @RequestBody CreateUtilisateurRequest request) {
        return ResponseEntity.ok(service.registerUtilisateur(request));
    }

    @PutMapping("/update-user/{id}")
    @PreAuthorize(SecurityRoles.ADMIN_CODEPULSE)
    public ResponseEntity<UtilisateurDto> modifierUtilisateur(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUtilisateurRequest request
    ) {
        return ResponseEntity.ok(service.modifierUtilisateur(request, id));
    }

    @PatchMapping("/promote-role/{id}")
    @PreAuthorize(SecurityRoles.ADMIN_CODEPULSE)
    public ResponseEntity<PromoteRoleResponse> promoteRole(
            @PathVariable Long id,
            @Valid @RequestBody PromoteRoleRequest request
    ) {
        return ResponseEntity.ok(service.promoteRole(id, request.role()));
    }

    @DeleteMapping("/delete-user/{id}")
    @PreAuthorize(SecurityRoles.ADMIN_CODEPULSE)
    public ResponseEntity<SuppressionResponse> supprimerUtilisateur(
            @PathVariable Long id
    ) {
        SuppressionResponse response = service.supprimerUtilisateur(id);
        return response.supprime()
                ? ResponseEntity.ok(response)
                : ResponseEntity.status(404).body(response);
    }

    @GetMapping("/get-user/{id}")
    @PreAuthorize(SecurityRoles.ADMIN_CODEPULSE)
    public ResponseEntity<UtilisateurDto> getUtilisateur(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(service.getUtilisateur(id));
    }

    @GetMapping("/get-all-users")
    @PreAuthorize(SecurityRoles.ADMIN_CODEPULSE)
    public ResponseEntity<List<UtilisateurDto>> getAllUtilisateurs() {
        return ResponseEntity.ok(service.getAllUtilisateurs());
    }

    @GetMapping("/get-user-by-email")
    @PreAuthorize(SecurityRoles.ADMIN_CODEPULSE)
    public ResponseEntity<UtilisateurDto> getUtilisateurByEmail(
            @RequestParam String email
    ) {
        return ResponseEntity.ok(service.getUtilisateurByEmail(email));
    }

    @GetMapping("/get-user-by-role")
    @PreAuthorize(SecurityRoles.ADMIN_CODEPULSE)
    public ResponseEntity<List<UtilisateurDto>> getUtilisateursByRole(
            @RequestParam Role role
    ) {
        return ResponseEntity.ok(service.getUtilisateursByRole(role));
    }

    @GetMapping("/get-users-pages/page")
    @PreAuthorize(SecurityRoles.ADMIN_CODEPULSE)
    public ResponseEntity<Page<UtilisateurDto>> getUtilisateursPage(
            @RequestParam @Min(1) int page,
            @RequestParam @Min(1) int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Role role
    ) {
        return ResponseEntity.ok(
                service.searchUtilisateurs(q, role, PaginationUtils.toSpringPageIndex(page), size)
        );
    }

    @GetMapping("/exists")
    @PreAuthorize(SecurityRoles.ADMIN_CODEPULSE)
    public ResponseEntity<Boolean> existsByEmail(
            @RequestParam String email
    ) {
        return ResponseEntity.ok(service.existsByEmail(email));
    }

    @GetMapping("/count")
    @PreAuthorize(SecurityRoles.ADMIN_CODEPULSE)
    public ResponseEntity<Long> countTotalUtilisateurs() {
        return ResponseEntity.ok(service.countTotalUtilisateurs());
    }

    @GetMapping("/count/role")
    @PreAuthorize(SecurityRoles.ADMIN_CODEPULSE)
    public ResponseEntity<Long> countUtilisateursByRole(
            @RequestParam Role role
    ) {
        return ResponseEntity.ok(service.countUtilisateursByRole(role));
    }
}
