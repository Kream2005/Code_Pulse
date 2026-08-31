package com.stage.backend.dto.feedback;

import com.stage.backend.dto.codingchallenge.CodingChallengeDto;
import com.stage.backend.dto.questionfeedback.QuestionFeedbackResponse;

import java.util.List;

public record FeedbackFormResponse(
        CodingChallengeDto challenge,
        List<QuestionFeedbackResponse> questions,
        boolean alreadySubmitted,
        Long feedbackId,
        Float noteGlobale,
        String commentaire,
        List<FeedbackDraftAnswerResponse> reponses
) {}