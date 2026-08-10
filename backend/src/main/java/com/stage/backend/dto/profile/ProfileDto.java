package com.stage.backend.dto.profile;

import com.stage.backend.enums.Role;

import java.time.ZonedDateTime;

public record ProfileDto(
        Long id,
        String nom,
        String prenom,
        Role role,
        String email,
        double hoursLoggedIn,
        ZonedDateTime lastLogin,
        ZonedDateTime createdAt,
        ZonedDateTime updatedAt,
        String pays
) {}
