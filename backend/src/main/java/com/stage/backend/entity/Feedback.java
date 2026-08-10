package com.stage.backend.entity;

import com.stage.backend.enums.StatutFeedback;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.ZonedDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity(name = "feedback")
public class Feedback {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;

    @Column(name = "note_globale")
    private Float noteGlobale;

    @Column(name = "commentaire")
    private String commentaire;

    @Column(name = "statut_feedback")
    @Enumerated(EnumType.STRING)
    private StatutFeedback statutFeedback;

    @Column(name = "created_at")
    private ZonedDateTime createdAt;

    /** Snapshot kept so admins can still read feedback after challenge soft-delete. */
    @Column(name = "challenge_titre")
    private String challengeTitre;

    @Column(name = "challenge_tag")
    private String challengeTag;

    @Column(name = "challenge_description", length = 4000)
    private String challengeDescription;

    @Column(name = "supprime", nullable = false)
    private boolean supprime = false;

    @ManyToOne(fetch = FetchType.LAZY)
    private Utilisateur utilisateur;

    @OneToOne(fetch = FetchType.LAZY)
    private CodingChallenge codingChallenge;
}
