package com.stage.backend.dto.feedback;

import com.stage.backend.enums.StatutFeedback;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SubmitFeedbackRequest(
        @NotNull Long codingChallengeId,
        @Min(0) @Max(5) Float noteGlobale,
        @Size(max = 5000) String commentaire,
        @NotNull StatutFeedback statut,
        @Valid List<AnswerRequest> reponses
) {
    public record AnswerRequest(
            @NotNull Long questionId,
            @Size(max = 5000) String valeur
    ) {}
}
