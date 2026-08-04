package com.stage.backend.dto.questionfeedback;

import com.stage.backend.enums.TypeQuestion;

import java.util.List;

public record UpdateQuestionFeedbackRequest(
        String libelle,
        TypeQuestion type,
        boolean obligatoire,
        List<String> choix
) {}
