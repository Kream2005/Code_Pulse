package com.stage.backend.mapper;

import com.stage.backend.dto.feedback.FeedbackResponse;
import com.stage.backend.entity.Feedback;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FeedbackMapper {

    @Mapping(target = "statut", source = "statutFeedback")
    @Mapping(target = "utilisateurId", source = "utilisateur.id")
    @Mapping(target = "utilisateurNom", source = "utilisateur.nom")
    @Mapping(target = "utilisateurPrenom", source = "utilisateur.prenom")
    @Mapping(target = "utilisateurUserName", source = "utilisateur.userName")
    @Mapping(target = "utilisateurEmail", source = "utilisateur.email")
    @Mapping(target = "codingChallengeId", source = "codingChallenge.id")
    @Mapping(target = "challengeSupprime", expression = "java(feedback.getCodingChallenge() != null && feedback.getCodingChallenge().isSupprime())")
    FeedbackResponse toResponseDto(Feedback feedback);

    Feedback toEntity(FeedbackResponse feedbackResponse);
}
