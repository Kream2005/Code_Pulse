package com.stage.backend.service.questionfeedback;

import com.stage.backend.dto.questionfeedback.CreateQuestionFeedbackRequest;
import com.stage.backend.dto.questionfeedback.QuestionFeedbackResponse;
import com.stage.backend.enums.TypeQuestion;
import org.springframework.data.domain.Page;

import java.util.List;

public interface QuestionFeedbackService {
    QuestionFeedbackResponse ajouterQuestion(CreateQuestionFeedbackRequest request);

    QuestionFeedbackResponse modifierQuestion(QuestionFeedbackResponse request, Long id);

    boolean supprimerQuestion(Long questionId);

    QuestionFeedbackResponse getQuestion(Long questionId);

    List<QuestionFeedbackResponse> getAllQuestions();

    Page<QuestionFeedbackResponse> getQuestionsPage(int page, int size);

    Page<QuestionFeedbackResponse> searchQuestions(String keyword, TypeQuestion type, int page, int size);

    List<QuestionFeedbackResponse> getQuestionsByType(TypeQuestion type);

    List<QuestionFeedbackResponse> getQuestionsByObligatoire(boolean obligatoire);

    long countQuestions();

    boolean existsById(Long questionId);
}