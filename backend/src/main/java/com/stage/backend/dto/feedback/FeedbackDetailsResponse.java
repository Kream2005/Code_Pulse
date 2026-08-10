package com.stage.backend.dto.feedback;

import com.stage.backend.dto.reponsefeedback.ReponseFeedbackResponse;

import java.util.List;

public record FeedbackDetailsResponse(
        FeedbackResponse feedback,
        List<ReponseFeedbackResponse> reponses
) {}
