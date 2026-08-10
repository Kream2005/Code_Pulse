package com.stage.backend.controlleur.feedback;
import com.stage.backend.dto.feedback.FeedbackDetailsResponse;
import com.stage.backend.dto.feedback.FeedbackFormResponse;
import com.stage.backend.dto.feedback.FeedbackResponse;
import com.stage.backend.dto.feedback.SubmitFeedbackRequest;
import com.stage.backend.enums.StatutFeedback;
import com.stage.backend.security.JwtUtils;
import com.stage.backend.security.SecurityRoles;
import com.stage.backend.service.feedback.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.ZonedDateTime;
import java.util.List;
@RestController
@RequestMapping("/feedbacks")
@RequiredArgsConstructor
public class FeedbackRestController {

    private final FeedbackService service;

    @PostMapping("/submit")
    @PreAuthorize(SecurityRoles.USER)
    public ResponseEntity<FeedbackResponse> submitFeedback(
            @Valid @RequestBody SubmitFeedbackRequest request
    ) {
        Long userId = JwtUtils.getCurrentUserId();
        return ResponseEntity.ok(service.submitFeedback(request, userId));
    }

    @GetMapping("/form")
    @PreAuthorize(SecurityRoles.USER)
    public ResponseEntity<FeedbackFormResponse> getFeedbackForm(
            @RequestParam Long challengeId
    ) {
        Long userId = JwtUtils.getCurrentUserId();
        return ResponseEntity.ok(service.getFeedbackForm(challengeId, userId));
    }

    @GetMapping("/get-feedback/{id}")
    @PreAuthorize(SecurityRoles.USER_OR_READ_FEEDBACKS)
    public ResponseEntity<FeedbackResponse> getFeedback(@PathVariable Long id) {
        return ResponseEntity.ok(service.getFeedback(id, JwtUtils.getCurrentUserId()));
    }

    @GetMapping("/details/{id}")
    @PreAuthorize(SecurityRoles.USER_OR_READ_FEEDBACKS)
    public ResponseEntity<FeedbackDetailsResponse> getFeedbackDetails(@PathVariable Long id) {
        return ResponseEntity.ok(service.getFeedbackDetails(id, JwtUtils.getCurrentUserId()));
    }

    @GetMapping("/get-all-feedacks")
    @PreAuthorize(SecurityRoles.READ_FEEDBACKS)
    public ResponseEntity<List<FeedbackResponse>> getAllFeedbacks() {
        return ResponseEntity.ok(service.getAllFeedbacks());
    }

    @GetMapping("/get-feedback-pages/page")
    @PreAuthorize(SecurityRoles.READ_FEEDBACKS)
    public ResponseEntity<Page<FeedbackResponse>> getFeedbacksPage(
            @RequestParam int page,
            @RequestParam int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) StatutFeedback statut,
            @RequestParam(required = false) String tag
    ) {
        return ResponseEntity.ok(service.searchFeedbacks(q, statut, tag, page, size));
    }

    @GetMapping("/get-feedback-by-statut")
    @PreAuthorize(SecurityRoles.READ_FEEDBACKS)
    public ResponseEntity<List<FeedbackResponse>> getFeedbacksByStatut(
            @RequestParam StatutFeedback statutFeedback
    ) {
        return ResponseEntity.ok(service.getFeedbacksByStatut(statutFeedback));
    }

    @GetMapping("/get-feedback-by-note-globale")
    @PreAuthorize(SecurityRoles.READ_FEEDBACKS)
    public ResponseEntity<List<FeedbackResponse>> getFeedbacksByNoteGlobale(
            @RequestParam Float noteGlobale
    ) {
        return ResponseEntity.ok(service.getFeedbacksByNoteGlobale(noteGlobale));
    }

    @GetMapping("/get-feedback-by-commentaire")
    @PreAuthorize(SecurityRoles.READ_FEEDBACKS)
    public ResponseEntity<List<FeedbackResponse>> getFeedbacksByCommentaire(
            @RequestParam String commentaire
    ) {
        return ResponseEntity.ok(service.getFeedbacksByCommentaire(commentaire));
    }

    @GetMapping("/get-feedback-by-utilisateur")
    @PreAuthorize(SecurityRoles.USER_OR_READ_FEEDBACKS)
    public ResponseEntity<List<FeedbackResponse>> getFeedbacksByUtilisateur(
            @RequestParam Long utilisateurId
    ) {
        Long currentUserId = JwtUtils.getCurrentUserId();
        if (!utilisateurId.equals(currentUserId) && !hasReadFeedbackRole()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return ResponseEntity.ok(service.getFeedbacksByUtilisateur(utilisateurId));
    }

    @GetMapping("/get-feedback-by-utilisateur-pages/page")
    @PreAuthorize(SecurityRoles.USER_OR_READ_FEEDBACKS)
    public ResponseEntity<Page<FeedbackResponse>> getFeedbacksByUtilisateurPage(
            @RequestParam Long utilisateurId,
            @RequestParam int page,
            @RequestParam int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) StatutFeedback statut,
            @RequestParam(required = false) String tag
    ) {
        Long currentUserId = JwtUtils.getCurrentUserId();
        if (!utilisateurId.equals(currentUserId) && !hasReadFeedbackRole()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return ResponseEntity.ok(service.searchFeedbacksByUtilisateur(utilisateurId, q, statut, tag, page, size));
    }

    @GetMapping("/get-feedback-by-created-at")
    @PreAuthorize(SecurityRoles.READ_FEEDBACKS)
    public ResponseEntity<List<FeedbackResponse>> getFeedbacksByCreatedAt(
            @RequestParam ZonedDateTime createdAt
    ) {
        return ResponseEntity.ok(service.getFeedbacksByCreatedAt(createdAt));
    }

    @GetMapping("/exists")
    @PreAuthorize(SecurityRoles.READ_FEEDBACKS)
    public ResponseEntity<Boolean> existsById(@RequestParam Long feedbackId) {
        return ResponseEntity.ok(service.existsById(feedbackId));
    }

    @GetMapping("/count-all-feedbacks")
    @PreAuthorize(SecurityRoles.READ_FEEDBACKS)
    public ResponseEntity<Long> countFeedbacks() {
        return ResponseEntity.ok(service.countFeedbacks());
    }

    @GetMapping("/count/statut")
    @PreAuthorize(SecurityRoles.READ_FEEDBACKS)
    public ResponseEntity<Long> countFeedbacksByStatut(@RequestParam StatutFeedback statutFeedback) {
        return ResponseEntity.ok(service.countFeedbacksByStatut(statutFeedback));
    }

    @GetMapping("/get-average-note")
    @PreAuthorize(SecurityRoles.READ_FEEDBACKS)
    public ResponseEntity<Float> getAverageNoteGlobale() {
        return ResponseEntity.ok(service.getAverageNoteGlobale());
    }

    private boolean hasReadFeedbackRole() {
        return org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN_CODING_CHALLENGE")
                        || a.getAuthority().equals("ROLE_MANAGER_RH")
                        || a.getAuthority().equals("ROLE_ADMIN_CODEPULSE"));
    }
}
