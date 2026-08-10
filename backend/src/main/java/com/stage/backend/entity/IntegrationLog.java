package com.stage.backend.entity;

import com.stage.backend.enums.StatutLog;
import com.stage.backend.enums.TypeLog;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.ZonedDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity(name = "integration_log")
public class IntegrationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private TypeLog type;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut")
    private StatutLog statut;

    @Column(name = "message", columnDefinition = "text")
    private String message;

    @Column(name = "date")
    private ZonedDateTime date;

    @ManyToOne
    private CodingChallenge codingChallenge;
}
