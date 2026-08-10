package com.stage.backend.entity;

import com.stage.backend.enums.StatutNotification;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.ZonedDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity(name = "notification")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;

    @Column(name = "date_envoi")
    private ZonedDateTime dateEnvoi;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut")
    private StatutNotification statut;

    @ManyToOne
    private Utilisateur utilisateur;

    @ManyToOne
    private CodingChallenge codingChallenge;

    @Column(name = "supprime", nullable = false)
    private boolean supprime = false;
}
