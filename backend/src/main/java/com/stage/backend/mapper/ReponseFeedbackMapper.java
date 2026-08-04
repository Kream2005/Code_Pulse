package com.stage.backend.mapper;

import com.stage.backend.dto.reponsefeedback.CreateReponseFeedbackRequest;
import com.stage.backend.dto.reponsefeedback.ReponseFeedbackResponse;
import com.stage.backend.entity.ReponseFeedback;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReponseFeedbackMapper {
    CreateReponseFeedbackRequest toCreateReponseFeedbackRequest(ReponseFeedback reponseFeedback);
    ReponseFeedback toEntity(CreateReponseFeedbackRequest createReponseFeedbackRequest);

    @Mapping(target = "questionFeedbackId", source = "questionFeedback.id")
    ReponseFeedbackResponse toReponseFeedbackResponse(ReponseFeedback reponseFeedback);

    ReponseFeedback toEntity(ReponseFeedbackResponse reponseFeedbackResponse);
}
