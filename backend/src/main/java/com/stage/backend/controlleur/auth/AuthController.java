package com.stage.backend.controlleur.auth;

import com.stage.backend.dto.demande.DemandeMotDePasseResponse;
import com.stage.backend.dto.demande.ForgotPasswordRequest;
import com.stage.backend.dto.demande.ResetInfoResponse;
import com.stage.backend.dto.login.LoginRequest;
import com.stage.backend.dto.login.LoginResponse;
import com.stage.backend.dto.login.ResetPasswordRequest;
import com.stage.backend.dto.utilisateur.CompleteAccountRequest;
import com.stage.backend.dto.utilisateur.SetupAccountInfoResponse;
import com.stage.backend.service.auth.AuthService;
import com.stage.backend.service.demande.DemandeReinitialisationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final DemandeReinitialisationService demandeReinitialisationService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request.email(), request.password());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/complete-account")
    public ResponseEntity<LoginResponse> completeAccount(
            @Valid @RequestBody CompleteAccountRequest request
    ) {
        return ResponseEntity.ok(authService.completeAccount(request));
    }

    @GetMapping("/setup-info")
    public ResponseEntity<SetupAccountInfoResponse> getSetupInfo(
            @RequestParam String token
    ) {
        return ResponseEntity.ok(authService.getSetupAccountInfo(token));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<DemandeMotDePasseResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        return ResponseEntity.ok(demandeReinitialisationService.soumettreDemande(request.email()));
    }

    @GetMapping("/reset-info")
    public ResponseEntity<ResetInfoResponse> resetInfo(@RequestParam String token) {
        return ResponseEntity.ok(demandeReinitialisationService.getResetInfo(token));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<LoginResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        return ResponseEntity.ok(
                demandeReinitialisationService.reinitialiserMotDePasse(request.token(), request.password())
        );
    }
}
