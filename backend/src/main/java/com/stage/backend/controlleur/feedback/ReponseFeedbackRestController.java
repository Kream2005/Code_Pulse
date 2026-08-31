package com.stage.backend.controlleur.feedback;

import com.stage.backend.dto.common.SuppressionResponse;
import com.stage.backend.dto.reponsefeedback.CreateReponseFeedbackRequest;
import com.stage.backend.dto.reponsefeedback.ReponseFeedbackResponse;
import com.stage.backend.security.SecurityRoles;
import com.stage.backend.service.reponsefeedback.ReponseFeedbackService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reponses-feedback")
@RequiredArgsConstructor
@Validated
public class ReponseFeedbackRestController {

    private final ReponseFeedbackService service;

    @PostMapping("/ajouter-reponse")
    @PreAuthorize(SecurityRoles.ADMIN_CHALLENGE)
    public ResponseEntity<ReponseFeedbackResponse> ajouterReponse(
            @Valid @RequestBody CreateReponseFeedbackRequest request
    ) {
        return ResponseEntity.ok(service.ajouterReponse(request));
    }

    @DeleteMapping("/delete-reponse/{id}")
    @PreAuthorize(SecurityRoles.ADMIN_CHALLENGE)
    public ResponseEntity<SuppressionResponse> supprimerReponse(
            @PathVariable Long id
    ) {
        SuppressionResponse response = service.supprimerReponse(id);
        return response.supprime()
                ? ResponseEntity.ok(response)
                : ResponseEntity.status(404).body(response);
    }

    @GetMapping("/get-reponse/{id}")
    @PreAuthorize(SecurityRoles.READ_FEEDBACKS)
    public ResponseEntity<ReponseFeedbackResponse> getReponse(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(service.getReponse(id));
    }

    @GetMapping("/get-all-reponses")
    @PreAuthorize(SecurityRoles.READ_FEEDBACKS)
    public ResponseEntity<List<ReponseFeedbackResponse>> getAllReponses() {
        return ResponseEntity.ok(service.getAllReponses());
    }

    @GetMapping("/get-reponses-pages/page")
    @PreAuthorize(SecurityRoles.READ_FEEDBACKS)
    public ResponseEntity<Page<ReponseFeedbackResponse>> getReponsesPage(
            @RequestParam @Min(0) int page,
            @RequestParam @Min(1) int size
    ) {
        return ResponseEntity.ok(service.getReponsesPage(page, size));
    }

    @GetMapping("/get-reponses-by-question")
    @PreAuthorize(SecurityRoles.READ_FEEDBACKS)
    public ResponseEntity<List<ReponseFeedbackResponse>> getReponsesByQuestion(
            @RequestParam @NotNull Long questionFeedbackId
    ) {
        return ResponseEntity.ok(service.getReponsesByQuestion(questionFeedbackId));
    }

    @GetMapping("/count")
    @PreAuthorize(SecurityRoles.READ_FEEDBACKS)
    public ResponseEntity<Long> countReponses() {
        return ResponseEntity.ok(service.countReponses());
    }

    @GetMapping("/exists")
    @PreAuthorize(SecurityRoles.READ_FEEDBACKS)
    public ResponseEntity<Boolean> existsById(
            @RequestParam @NotNull Long reponseId
    ) {
        return ResponseEntity.ok(service.existsById(reponseId));
    }
}
