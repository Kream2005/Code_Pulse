package com.stage.backend.dto.questionfeedback;

import com.stage.backend.enums.TypeQuestion;

import java.util.List;

public record QuestionFeedbackResponse(
        Long id,
        String libelle,
        TypeQuestion type,
        boolean obligatoire,
        List<String> choix
) {}
