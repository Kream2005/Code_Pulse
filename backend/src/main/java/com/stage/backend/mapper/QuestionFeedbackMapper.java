package com.stage.backend.mapper;

import com.stage.backend.dto.questionfeedback.CreateQuestionFeedbackRequest;
import com.stage.backend.dto.questionfeedback.QuestionFeedbackResponse;
import com.stage.backend.dto.questionfeedback.UpdateQuestionFeedbackRequest;
import com.stage.backend.entity.QuestionFeedback;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface QuestionFeedbackMapper {
    CreateQuestionFeedbackRequest toQuestionFeedbackDto(QuestionFeedback questionFeedback);
    QuestionFeedback toEntity(CreateQuestionFeedbackRequest createQuestionFeedbackRequest);

    QuestionFeedbackResponse toQuestionFeedbackResponse(QuestionFeedback questionFeedback);
    QuestionFeedback toEntity(QuestionFeedbackResponse questionFeedbackResponse);

    UpdateQuestionFeedbackRequest toUpdateQuestionFeedbackRequest(QuestionFeedback questionFeedback);
    QuestionFeedback toEntity(UpdateQuestionFeedbackRequest updateQuestionFeedbackRequest);
}
