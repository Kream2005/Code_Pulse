package com.stage.backend.dto.reponsefeedback;

public record ReponseFeedbackResponse(
        Long id,
        String valeur,
        Long questionFeedbackId,
        Long feedbackId
) {}
