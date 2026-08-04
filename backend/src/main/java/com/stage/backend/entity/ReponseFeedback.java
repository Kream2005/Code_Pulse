package com.stage.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity(name = "reponse_feedback")
public class ReponseFeedback {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;

    @Column(name = "valeur")
    private String valeur;

    @ManyToOne
    private QuestionFeedback questionFeedback;

    @Column(name = "feedback_id")
    private Long feedbackId;

    @Column(name = "supprime", nullable = false)
    private boolean supprime = false;
}
