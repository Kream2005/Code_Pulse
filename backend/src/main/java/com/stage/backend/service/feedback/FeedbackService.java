package com.stage.backend.service.feedback;

import com.stage.backend.dto.feedback.FeedbackDetailsResponse;
import com.stage.backend.dto.feedback.FeedbackFormResponse;
import com.stage.backend.dto.feedback.FeedbackResponse;
import com.stage.backend.dto.feedback.SubmitFeedbackRequest;
import com.stage.backend.enums.StatutFeedback;
import org.springframework.data.domain.Page;

import java.time.ZonedDateTime;
import java.util.List;

public interface FeedbackService {

    FeedbackResponse submitFeedback(SubmitFeedbackRequest request, Long utilisateurId);

    FeedbackFormResponse getFeedbackForm(Long codingChallengeId, Long utilisateurId);

    FeedbackResponse getFeedback(Long feedbackId, Long currentUserId);

    FeedbackDetailsResponse getFeedbackDetails(Long feedbackId, Long currentUserId);

    List<FeedbackResponse> getAllFeedbacks();

    Page<FeedbackResponse> getFeedbacksPage(int page, int size);

    List<FeedbackResponse> getFeedbacksByStatut(StatutFeedback statutFeedback);

    List<FeedbackResponse> getFeedbacksByNoteGlobale(Float noteGlobale);

    List<FeedbackResponse> getFeedbacksByCommentaire(String commentaire);

    List<FeedbackResponse> getFeedbacksByUtilisateur(Long utilisateurId);

    Page<FeedbackResponse> getFeedbacksByUtilisateurPage(Long utilisateurId, int page, int size);

    List<FeedbackResponse> getFeedbacksByCreatedAt(ZonedDateTime createdAt);

    long countFeedbacks();

    long countFeedbacksByStatut(StatutFeedback statutFeedback);

    Float getAverageNoteGlobale();

    boolean existsById(Long feedbackId);
}
