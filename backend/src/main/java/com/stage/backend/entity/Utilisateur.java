package com.stage.backend.entity;

import com.stage.backend.enums.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.ZonedDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity(name = "utilisateur")
public class Utilisateur {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;

    @Column(name = "external_id", unique = true)
    private Long externalId;

    @Column(name = "nom")
    private String nom;

    @Column(name = "prenom")
    private String prenom;

    @Column(name = "username", unique = true)
    private String userName;

    @Column(name = "email")
    private String email;

    @Column(name = "password")
    private String password;

    @Column(name = "status")
    private Boolean status;

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Role role;

    @Column(name = "compte_complet")
    private boolean compteComplet = false;

    @Column(name = "setup_token")
    private String setupToken;

    @Column(name = "setup_token_expires_at")
    private ZonedDateTime setupTokenExpiresAt;

    /** Soft-delete: feedbacks remain accessible for admin audit. */
    @Column(name = "supprime", nullable = false)
    private boolean supprime = false;

    @OneToMany(mappedBy = "utilisateur")
    private List<Feedback> feedbackList;

    @OneToMany(mappedBy = "utilisateur")
    private List<Notification> notificationList;
}
