package com.stage.backend.controlleur.feedback;

import com.stage.backend.dto.common.SuppressionResponse;
import com.stage.backend.dto.questionfeedback.CreateQuestionFeedbackRequest;
import com.stage.backend.dto.questionfeedback.QuestionFeedbackResponse;
import com.stage.backend.enums.TypeQuestion;
import com.stage.backend.security.SecurityRoles;
import com.stage.backend.service.questionfeedback.QuestionFeedbackService;
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
@RequestMapping("/questions-feedback")
@RequiredArgsConstructor
@Validated
public class QuestionFeedbackRestController {

    private final QuestionFeedbackService service;

    @PostMapping("/add-question")
    @PreAuthorize(SecurityRoles.MANAGE_QUESTIONS)
    public ResponseEntity<QuestionFeedbackResponse> ajouterQuestion(
            @Valid @RequestBody CreateQuestionFeedbackRequest request
    ) {
        return ResponseEntity.ok(service.ajouterQuestion(request));
    }

    @PutMapping("/update-question/{id}")
    @PreAuthorize(SecurityRoles.MANAGE_QUESTIONS)
    public ResponseEntity<QuestionFeedbackResponse> modifierQuestion(
            @PathVariable Long id,
            @Valid @RequestBody QuestionFeedbackResponse request
    ) {
        return ResponseEntity.ok(service.modifierQuestion(request, id));
    }

    @DeleteMapping("/delete-question/{id}")
    @PreAuthorize(SecurityRoles.MANAGE_QUESTIONS)
    public ResponseEntity<SuppressionResponse> supprimerQuestion(
            @PathVariable Long id
    ) {
        SuppressionResponse response = service.supprimerQuestion(id);
        return response.supprime()
                ? ResponseEntity.ok(response)
                : ResponseEntity.status(404).body(response);
    }

    @GetMapping("/get-question/{id}")
    @PreAuthorize(SecurityRoles.MANAGE_QUESTIONS)
    public ResponseEntity<QuestionFeedbackResponse> getQuestion(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(service.getQuestion(id));
    }

    @GetMapping("/get-all-questions")
    @PreAuthorize(SecurityRoles.MANAGE_QUESTIONS)
    public ResponseEntity<List<QuestionFeedbackResponse>> getAllQuestions() {
        return ResponseEntity.ok(service.getAllQuestions());
    }

    @GetMapping("/get-questions-pages/page")
    @PreAuthorize(SecurityRoles.MANAGE_QUESTIONS)
    public ResponseEntity<Page<QuestionFeedbackResponse>> getQuestionsPage(
            @RequestParam @Min(0) int page,
            @RequestParam @Min(1) int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) TypeQuestion type
    ) {
        return ResponseEntity.ok(service.searchQuestions(q, type, page, size));
    }

    @GetMapping("/get-questions-by-type/type")
    @PreAuthorize(SecurityRoles.MANAGE_QUESTIONS)
    public ResponseEntity<List<QuestionFeedbackResponse>> getQuestionsByType(
            @RequestParam TypeQuestion type
    ) {
        return ResponseEntity.ok(service.getQuestionsByType(type));
    }

    @GetMapping("/get-questions-by-obligatoire")
    @PreAuthorize(SecurityRoles.MANAGE_QUESTIONS)
    public ResponseEntity<List<QuestionFeedbackResponse>> getQuestionsByObligatoire(
            @RequestParam boolean obligatoire
    ) {
        return ResponseEntity.ok(service.getQuestionsByObligatoire(obligatoire));
    }

    @GetMapping("/count-questions")
    @PreAuthorize(SecurityRoles.MANAGE_QUESTIONS)
    public ResponseEntity<Long> countQuestions() {
        return ResponseEntity.ok(service.countQuestions());
    }

    @GetMapping("/exists")
    @PreAuthorize(SecurityRoles.MANAGE_QUESTIONS)
    public ResponseEntity<Boolean> existsById(
            @RequestParam @NotNull Long questionId
    ) {
        return ResponseEntity.ok(service.existsById(questionId));
    }
}
