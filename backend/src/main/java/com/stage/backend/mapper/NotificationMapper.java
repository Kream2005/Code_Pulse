package com.stage.backend.mapper;

import com.stage.backend.dto.notification.CreateNotificationRequest;
import com.stage.backend.dto.notification.NotificationDetailsDto;
import com.stage.backend.dto.notification.NotificationDto;
import com.stage.backend.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {
    CreateNotificationRequest toCreateNotificationRequest(Notification request);
    Notification toEntity(CreateNotificationRequest createNotificationRequest);

    @Mapping(target = "utilisateurNom", source = "utilisateur.nom")
    @Mapping(target = "utilisateurEmail", source = "utilisateur.email")
    @Mapping(target = "challengeTitre", source = "codingChallenge.titre")
    NotificationDetailsDto toNotificationDetailsDto(Notification notification);

    Notification toEntity(NotificationDetailsDto notificationDetailsDto);

    Notification toEntity(NotificationDto notificationDto);

    @Mapping(target = "utilisateurId", source = "utilisateur.id")
    @Mapping(target = "codingChallengeId", source = "codingChallenge.id")
    @Mapping(target = "challengeTitre", source = "codingChallenge.titre")
    @Mapping(target = "challengeTag", source = "codingChallenge.tag")
    @Mapping(target = "challengeDuree", source = "codingChallenge.duree")
    @Mapping(target = "challengeDescription", source = "codingChallenge.description")
    NotificationDto toNotificationDto(Notification notification);

}
