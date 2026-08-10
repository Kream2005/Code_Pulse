package com.stage.backend.dto.reponsefeedback;

import com.stage.backend.enums.TypeQuestion;

import java.util.List;

public record ReponseFeedbackResponse(
        Long id,
        String valeur,
        Long questionFeedbackId,
        Long feedbackId,
        String questionLibelle,
        TypeQuestion questionType,
        boolean questionObligatoire,
        List<String> questionChoix,
        boolean questionSupprime
) {}
