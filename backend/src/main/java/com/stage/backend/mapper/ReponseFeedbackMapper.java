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
    @Mapping(target = "questionLibelle", source = "questionFeedback.libelle")
    @Mapping(target = "questionType", source = "questionFeedback.type")
    @Mapping(target = "questionObligatoire", source = "questionFeedback.obligatoire")
    @Mapping(target = "questionChoix", source = "questionFeedback.choix")
    @Mapping(target = "questionSupprime", source = "questionFeedback.supprime")
    ReponseFeedbackResponse toReponseFeedbackResponse(ReponseFeedback reponseFeedback);

    @Mapping(target = "questionFeedback", ignore = true)
    @Mapping(target = "supprime", ignore = true)
    ReponseFeedback toEntity(ReponseFeedbackResponse reponseFeedbackResponse);
}
