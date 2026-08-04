package com.stage.backend.mapper;

import com.stage.backend.dto.utilisateur.CreateUtilisateurRequest;
import com.stage.backend.dto.utilisateur.UpdateUtilisateurRequest;
import com.stage.backend.dto.utilisateur.UtilisateurDto;
import com.stage.backend.entity.Utilisateur;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UtilisateurMapper {
    CreateUtilisateurRequest toCreateUtilisateurRequest(Utilisateur utilisateur);
    Utilisateur toEntity(CreateUtilisateurRequest createUtilisateurRequest);

    UtilisateurDto toUtilisateurDto(Utilisateur utilisateur);
    Utilisateur toEntity(UtilisateurDto utilisateurDto);

    UpdateUtilisateurRequest toUpdateUtilisateurRequest(Utilisateur utilisateur);
    Utilisateur toEntity(UpdateUtilisateurRequest updateUtilisateurRequest);
}
