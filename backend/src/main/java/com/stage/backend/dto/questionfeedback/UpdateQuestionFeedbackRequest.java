package com.stage.backend.dto.questionfeedback;

import com.stage.backend.enums.TypeQuestion;

public record UpdateQuestionFeedbackRequest(
        String libelle,
        TypeQuestion type,
        boolean obligatoire
) {}