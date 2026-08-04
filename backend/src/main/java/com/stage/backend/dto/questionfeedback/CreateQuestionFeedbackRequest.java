package com.stage.backend.dto.questionfeedback;

import com.stage.backend.enums.TypeQuestion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateQuestionFeedbackRequest(
        @NotBlank @Size(max = 500) String libelle,
        @NotNull TypeQuestion type,
        boolean obligatoire
) {}
