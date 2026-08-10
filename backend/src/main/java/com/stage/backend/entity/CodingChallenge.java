package com.stage.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.ZonedDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity(name = "coding_challenge")
public class CodingChallenge {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "external_id", unique = true)
    private Long externalId;

    @Column(name = "titre")
    private String titre;

    @Column(name = "description")
    private String description;

    @Column(name = "tag")
    private String tag;

    @Column(name = "duree")
    private Integer duree;

    @Column(name = "code_url")
    private String codeUrl;

    @Column(name = "parameter")
    private Boolean parameter;

    // not provided by the api
    @Column(name = "date_completion")
    private ZonedDateTime dateCompletion;

    /** Soft-delete: feedbacks/notifications stay linked and readable by admins. */
    @Column(name = "supprime", nullable = false)
    private boolean supprime = false;

    @OneToMany(mappedBy = "codingChallenge")
    private List<Notification> notificationList;

    @OneToMany(mappedBy = "codingChallenge")
    private List<IntegrationLog> integrationLogList;
}
