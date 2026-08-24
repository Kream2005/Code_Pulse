package com.stage.backend.service.feedback;

import com.stage.backend.dto.feedback.SubmitFeedbackRequest;
import com.stage.backend.entity.CodingChallenge;
import com.stage.backend.entity.Feedback;
import com.stage.backend.entity.Notification;
import com.stage.backend.entity.QuestionFeedback;
import com.stage.backend.entity.Utilisateur;
import com.stage.backend.enums.Role;
import com.stage.backend.enums.StatutFeedback;
import com.stage.backend.enums.StatutNotification;
import com.stage.backend.enums.TypeQuestion;
import com.stage.backend.mapper.CodingChallengeMapper;
import com.stage.backend.mapper.FeedbackMapper;
import com.stage.backend.mapper.QuestionFeedbackMapper;
import com.stage.backend.mapper.ReponseFeedbackMapper;
import com.stage.backend.repository.CodingChallengeRepository;
import com.stage.backend.repository.FeedbackRepository;
import com.stage.backend.repository.NotificationRepository;
import com.stage.backend.repository.QuestionFeedbackRepository;
import com.stage.backend.repository.ReponseFeedbackRepository;
import com.stage.backend.repository.UtilisateurRepository;
import com.stage.backend.service.integrationlog.IntegrationLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.stage.backend.exception.FeedbackValidationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceImpTest {

    @Mock private FeedbackRepository repository;
    @Mock private FeedbackMapper mapper;
    @Mock private CodingChallengeRepository codingChallengeRepository;
    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private QuestionFeedbackRepository questionFeedbackRepository;
    @Mock private ReponseFeedbackRepository reponseFeedbackRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private CodingChallengeMapper codingChallengeMapper;
    @Mock private QuestionFeedbackMapper questionFeedbackMapper;
    @Mock private ReponseFeedbackMapper reponseFeedbackMapper;
    @Mock private IntegrationLogService integrationLogService;

    @InjectMocks
    private FeedbackServiceImp service;

    private Utilisateur user;
    private CodingChallenge challenge;
    private QuestionFeedback mandatoryQuestion;

    @BeforeEach
    void setUp() {
        user = new Utilisateur();
        user.setId(1L);
        user.setEmail("user@test.com");
        user.setRole(Role.USER);

        challenge = new CodingChallenge();
        challenge.setId(10L);
        challenge.setTitre("Test Challenge");

        mandatoryQuestion = new QuestionFeedback();
        mandatoryQuestion.setId(100L);
        mandatoryQuestion.setLibelle("Rate difficulty");
        mandatoryQuestion.setType(TypeQuestion.TEXTE);
        mandatoryQuestion.setObligatoire(true);
    }

    @Test
    void submitFeedback_duplicate_returns409() {
        when(repository.existsByUtilisateurIdAndCodingChallengeId(1L, 10L)).thenReturn(true);
        when(codingChallengeRepository.findById(10L)).thenReturn(Optional.of(challenge));

        SubmitFeedbackRequest request = new SubmitFeedbackRequest(
                10L, 4f, "ok", StatutFeedback.SOUMIS, List.of()
        );

        assertThatThrownBy(() -> service.submitFeedback(request, 1L))
                .isInstanceOf(FeedbackValidationException.class);
    }

    @Test
    void submitFeedback_missingMandatoryAnswer_returns400() {
        when(repository.existsByUtilisateurIdAndCodingChallengeId(1L, 10L)).thenReturn(false);
        when(codingChallengeRepository.findById(10L)).thenReturn(Optional.of(challenge));
        when(questionFeedbackRepository.findByObligatoire(true)).thenReturn(List.of(mandatoryQuestion));

        SubmitFeedbackRequest request = new SubmitFeedbackRequest(
                10L, 4f, "ok", StatutFeedback.SOUMIS, List.of()
        );

        assertThatThrownBy(() -> service.submitFeedback(request, 1L))
                .isInstanceOf(FeedbackValidationException.class);
    }

    @Test
    void submitFeedback_happyPath_marksNotificationAsLue() {
        when(repository.existsByUtilisateurIdAndCodingChallengeId(1L, 10L)).thenReturn(false);
        when(codingChallengeRepository.findById(10L)).thenReturn(Optional.of(challenge));
        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(user));
        when(questionFeedbackRepository.findByObligatoire(true)).thenReturn(List.of(mandatoryQuestion));
        when(questionFeedbackRepository.findById(100L)).thenReturn(Optional.of(mandatoryQuestion));

        Feedback saved = new Feedback();
        saved.setId(50L);
        saved.setUtilisateur(user);
        saved.setCodingChallenge(challenge);
        saved.setStatutFeedback(StatutFeedback.SOUMIS);
        when(repository.save(any(Feedback.class))).thenReturn(saved);

        Notification notification = new Notification();
        notification.setStatut(StatutNotification.EN_ATTENTE);
        when(notificationRepository.findByUtilisateurIdAndCodingChallengeId(1L, 10L))
                .thenReturn(Optional.of(notification));

        SubmitFeedbackRequest request = new SubmitFeedbackRequest(
                10L,
                4f,
                "Great challenge",
                StatutFeedback.SOUMIS,
                List.of(new SubmitFeedbackRequest.AnswerRequest(100L, "Easy"))
        );

        service.submitFeedback(request, 1L);

        assertThat(notification.getStatut()).isEqualTo(StatutNotification.LUE);
        verify(notificationRepository).save(notification);
        verify(integrationLogService).logEvent(any(), any(), any(), eq(10L));
    }
}
