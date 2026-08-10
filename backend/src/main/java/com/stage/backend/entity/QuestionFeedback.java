package com.stage.backend.entity;

import com.stage.backend.converter.StringListJsonConverter;
import com.stage.backend.enums.TypeQuestion;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity(name = "question_feedback")
public class QuestionFeedback {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;

    @Column(name = "libelle")
    private String libelle;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private TypeQuestion type;

    @Column(name = "obligatoire")
    private boolean obligatoire;

    /** Options for CHOIX questions, stored as JSON text. Empty for NOTE/TEXTE. */
    @Convert(converter = StringListJsonConverter.class)
    @Column(name = "choix", columnDefinition = "text")
    private List<String> choix = new ArrayList<>();

    /** Soft-delete: existing answers stay linked for history. */
    @Column(name = "supprime", nullable = false)
    private boolean supprime = false;

    @OneToMany(mappedBy = "questionFeedback")
    private List<ReponseFeedback> reponseFeedbackList;
}
