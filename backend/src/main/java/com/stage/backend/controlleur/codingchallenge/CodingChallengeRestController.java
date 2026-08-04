package com.stage.backend.controlleur.codingchallenge;

import com.stage.backend.dto.codingchallenge.CodingChallengeDto;
import com.stage.backend.kafka.event.CodingChallengeEvent;
import com.stage.backend.security.SecurityRoles;
import com.stage.backend.service.codingchallenge.CodingChallengeService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/coding-challenges")
@RequiredArgsConstructor
@Validated
public class CodingChallengeRestController {

    private final CodingChallengeService service;

    @PostMapping("/synchroniser")
    @PreAuthorize(SecurityRoles.ADMIN_CHALLENGE)
    public ResponseEntity<Map<String, Integer>> synchroniserChallenge() {
        int published = service.synchroniserChallenges();
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("published", published));
    }

    @PostMapping("/ingest-batch")
    @PreAuthorize(SecurityRoles.ADMIN_CHALLENGE)
    public ResponseEntity<Map<String, Integer>> ingestBatch(@RequestBody List<CodingChallengeEvent> events) {
        int processed = service.ingestBatch(events);
        return ResponseEntity.ok(Map.of("processed", processed));
    }

    @DeleteMapping("/delete-coding-challenge/{id}")
    @PreAuthorize(SecurityRoles.ADMIN_CHALLENGE)
    public ResponseEntity<Boolean> supprimerCodingChallenge(@PathVariable Long id) {
        boolean deleted = service.supprimerCodingChallenge(id);
        return deleted ? ResponseEntity.ok(true) : ResponseEntity.notFound().build();
    }

    @GetMapping("/get-coding-challenge/{id}")
    @PreAuthorize(SecurityRoles.AUTHENTICATED)
    public ResponseEntity<CodingChallengeDto> getCodingChallenge(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(service.getCodingChallenge(id));
    }

    @GetMapping("/get-all-coding-challenges")
    @PreAuthorize(SecurityRoles.READ_FEEDBACKS)
    public ResponseEntity<List<CodingChallengeDto>> getAllCodingChallenges() {
        return ResponseEntity.ok(service.getAllCodingChallenges());
    }

    @GetMapping("/get-coding-challenges-pages/page")
    @PreAuthorize(SecurityRoles.READ_FEEDBACKS)
    public ResponseEntity<Page<CodingChallengeDto>> getCodingChallengesPage(
            @RequestParam @Min(0) int page,
            @RequestParam @Min(1) int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String tag
    ) {
        return ResponseEntity.ok(service.searchCodingChallenges(q, tag, page, size));
    }

    @GetMapping("/get-coding-challenge-titre")
    @PreAuthorize(SecurityRoles.READ_FEEDBACKS)
    public ResponseEntity<List<CodingChallengeDto>> rechercherChallengesByTitre(
            @RequestParam @NotBlank String titre
    ) {
        return ResponseEntity.ok(service.rechercherChallengesByTitre(titre));
    }

    @GetMapping("/get-coding-challenge-by-description")
    @PreAuthorize(SecurityRoles.READ_FEEDBACKS)
    public ResponseEntity<List<CodingChallengeDto>> getChallengesByDescription(
            @RequestParam @NotBlank String description
    ) {
        return ResponseEntity.ok(service.getChallengesByDescription(description));
    }

    @GetMapping("/get-coding-challenge-by-duree")
    @PreAuthorize(SecurityRoles.READ_FEEDBACKS)
    public ResponseEntity<List<CodingChallengeDto>> getChallengesByDuree(
            @RequestParam @NotNull Integer duree
    ) {
        return ResponseEntity.ok(service.getChallengesByDuree(duree));
    }

    @GetMapping("/get-coding-challenge-by-date-completion")
    @PreAuthorize(SecurityRoles.READ_FEEDBACKS)
    public ResponseEntity<List<CodingChallengeDto>> getChallengesByDateCompletion(
            @RequestParam ZonedDateTime dateCompletion
    ) {
        return ResponseEntity.ok(service.getChallengesByDateCompletion(dateCompletion));
    }

    @GetMapping("/count-coding-challenges")
    @PreAuthorize(SecurityRoles.READ_FEEDBACKS)
    public ResponseEntity<Long> countCodingChallenges() {
        return ResponseEntity.ok(service.countCodingChallenges());
    }
}
