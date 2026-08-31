package com.stage.backend.controlleur.codingchallenge;

import com.stage.backend.dto.codingchallenge.ChallengeDeleteResponse;
import com.stage.backend.dto.codingchallenge.ChallengeIngestBatchResponse;
import com.stage.backend.dto.codingchallenge.ChallengeSyncResponse;
import com.stage.backend.dto.codingchallenge.CodingChallengeDto;
import com.stage.backend.kafka.event.CodingChallengeEvent;
import com.stage.backend.security.SecurityRoles;
import com.stage.backend.service.codingchallenge.CodingChallengeService;
import com.stage.backend.util.PaginationUtils;
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

@RestController
@RequestMapping("/coding-challenges")
@RequiredArgsConstructor
@Validated
public class CodingChallengeRestController {

    private final CodingChallengeService service;

    @PostMapping("/synchroniser")
    @PreAuthorize(SecurityRoles.ADMIN_CHALLENGE)
    public ResponseEntity<ChallengeSyncResponse> synchroniserChallenge() {
        ChallengeSyncResponse response = service.synchroniserChallenges();
        HttpStatus status;
        if ("erreur".equals(response.mode())) {
            status = HttpStatus.BAD_GATEWAY;
        } else if (response.failed() > 0) {
            status = HttpStatus.MULTI_STATUS;
        } else if ("desactive".equals(response.mode()) || "indisponible".equals(response.mode())) {
            status = HttpStatus.OK;
        } else {
            status = HttpStatus.ACCEPTED;
        }
        return ResponseEntity.status(status).body(response);
    }

    @PostMapping("/ingest-batch")
    @PreAuthorize(SecurityRoles.ADMIN_CHALLENGE)
    public ResponseEntity<ChallengeIngestBatchResponse> ingestBatch(@RequestBody List<CodingChallengeEvent> events) {
        ChallengeIngestBatchResponse response = service.ingestBatch(events);
        HttpStatus status = response.failed() > 0 ? HttpStatus.MULTI_STATUS : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }

    @DeleteMapping("/delete-coding-challenge/{id}")
    @PreAuthorize(SecurityRoles.ADMIN_CHALLENGE)
    public ResponseEntity<ChallengeDeleteResponse> supprimerCodingChallenge(@PathVariable Long id) {
        ChallengeDeleteResponse response = service.supprimerCodingChallenge(id);
        return response.deleted()
                ? ResponseEntity.ok(response)
                : ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
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
            @RequestParam @Min(1) int page,
            @RequestParam @Min(1) int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String tag
    ) {
        return ResponseEntity.ok(
                service.searchCodingChallenges(q, tag, PaginationUtils.toSpringPageIndex(page), size)
        );
    }

    @GetMapping("/tags")
    @PreAuthorize(SecurityRoles.AUTHENTICATED)
    public ResponseEntity<List<String>> getDistinctTags() {
        return ResponseEntity.ok(service.getDistinctTags());
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
