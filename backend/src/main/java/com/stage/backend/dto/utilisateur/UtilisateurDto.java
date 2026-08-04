package com.stage.backend.dto.utilisateur;

import com.stage.backend.enums.Role;

public record UtilisateurDto(
      Long id,
      String nom,
      String prenom,
      String email,
      Role role
) {}
