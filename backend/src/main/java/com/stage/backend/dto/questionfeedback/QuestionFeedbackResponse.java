package com.stage.backend.dto.questionfeedback;

import com.stage.backend.enums.TypeQuestion;

public record QuestionFeedbackResponse(
        Long id,
        String libelle,
        TypeQuestion type,
        boolean obligatoire
) {}
