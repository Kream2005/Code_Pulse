package com.stage.backend.service.feedback;
import com.stage.backend.dto.common.ErreurValidation;
import com.stage.backend.dto.feedback.FeedbackDraftAnswerResponse;
import com.stage.backend.dto.feedback.FeedbackDetailsResponse;
import com.stage.backend.dto.feedback.FeedbackFormResponse;
import com.stage.backend.dto.feedback.FeedbackResponse;
import com.stage.backend.dto.feedback.SubmitFeedbackRequest;
import com.stage.backend.entity.CodingChallenge;
import com.stage.backend.entity.Feedback;
import com.stage.backend.entity.QuestionFeedback;
import com.stage.backend.entity.ReponseFeedback;
import com.stage.backend.entity.Utilisateur;
import com.stage.backend.enums.StatutFeedback;
import com.stage.backend.enums.StatutLog;
import com.stage.backend.enums.StatutNotification;
import com.stage.backend.enums.TypeLog;
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
import com.stage.backend.exception.FeedbackValidationException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.stage.backend.service.integrationlog.IntegrationLogService;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Optional;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class FeedbackServiceImp implements FeedbackService {
    private final FeedbackRepository repository;
    private final FeedbackMapper mapper;
    private final CodingChallengeRepository codingChallengeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final QuestionFeedbackRepository questionFeedbackRepository;
    private final ReponseFeedbackRepository reponseFeedbackRepository;
    private final NotificationRepository notificationRepository;
    private final CodingChallengeMapper codingChallengeMapper;
    private final QuestionFeedbackMapper questionFeedbackMapper;
    private final ReponseFeedbackMapper reponseFeedbackMapper;
    private final IntegrationLogService integrationLogService;

    @Override
    public FeedbackResponse submitFeedback(SubmitFeedbackRequest request, Long utilisateurId) {
        List<ErreurValidation> erreurs = new ArrayList<>();

        CodingChallenge challenge = codingChallengeRepository.findById(request.codingChallengeId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Coding challenge introuvable : id=" + request.codingChallengeId()
                ));

        Optional<Feedback> existingOpt = findActiveFeedback(utilisateurId, request.codingChallengeId());
        assertFeedbackRecipient(utilisateurId, request.codingChallengeId());

        if (existingOpt.isPresent() && isSubmitted(existingOpt.get())) {
            integrationLogService.logEvent(
                    TypeLog.FEEDBACK,
                    StatutLog.WARNING,
                    "Tentative de doublon feedback pour le challenge " + request.codingChallengeId(),
                    request.codingChallengeId()
            );
            erreurs.add(new ErreurValidation(
                    "codingChallengeId",
                    null,
                    "DOUBLON",
                    "Un feedback a déjà été soumis pour ce coding challenge"
            ));
        } else if (existingOpt.isEmpty()
                && repository.existsByCodingChallengeId(request.codingChallengeId())) {
            integrationLogService.logEvent(
                    TypeLog.FEEDBACK,
                    StatutLog.WARNING,
                    "Tentative de doublon feedback pour le challenge " + request.codingChallengeId(),
                    request.codingChallengeId()
            );
            erreurs.add(new ErreurValidation(
                    "codingChallengeId",
                    null,
                    "DOUBLON",
                    "Un feedback a déjà été soumis pour ce coding challenge"
            ));
        }

        if (challenge.isSupprime()) {
            erreurs.add(new ErreurValidation(
                    "codingChallengeId",
                    null,
                    "CHALLENGE_ARCHIVE",
                    "Ce coding challenge a été archivé"
            ));
        }

        if (request.statut() == StatutFeedback.SOUMIS) {
            collecterErreursQuestionsObligatoires(erreurs, request);
            collecterErreursFormatsReponses(erreurs, request);
        }

        if (!erreurs.isEmpty()) {
            throw new FeedbackValidationException(
                    erreurs.size() + " erreur(s) de validation sur la soumission du feedback",
                    erreurs
            );
        }

        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Utilisateur introuvable : id=" + utilisateurId
                ));

        Feedback feedback = existingOpt.orElseGet(Feedback::new);
        if (feedback.getId() == null) {
            feedback.setUtilisateur(utilisateur);
            feedback.setCodingChallenge(challenge);
            feedback.setChallengeTitre(challenge.getTitre());
            feedback.setChallengeTag(challenge.getTag());
            feedback.setChallengeDescription(challenge.getDescription());
            feedback.setCreatedAt(ZonedDateTime.now());
        }
        feedback.setNoteGlobale(request.noteGlobale());
        feedback.setCommentaire(request.commentaire());
        feedback.setStatutFeedback(request.statut());
        Feedback saved = repository.save(feedback);

        upsertReponses(saved.getId(), request.reponses());
        markNotificationRead(utilisateurId, challenge.getId());

        integrationLogService.logEvent(
                TypeLog.FEEDBACK,
                StatutLog.SUCCES,
                "Feedback "
                        + request.statut()
                        + " by user "
                        + utilisateurId
                        + " for challenge "
                        + request.codingChallengeId(),
                request.codingChallengeId()
        );
        log.info("Feedback saved — user={} challenge={} statut={}",
                utilisateurId, request.codingChallengeId(), request.statut());
        return mapper.toResponseDto(saved);
    }

    private void upsertReponses(Long feedbackId, List<SubmitFeedbackRequest.AnswerRequest> reponses) {
        if (reponses == null) {
            return;
        }
        List<ReponseFeedback> existing = reponseFeedbackRepository.findByFeedbackId(feedbackId);
        if (!existing.isEmpty()) {
            reponseFeedbackRepository.deleteAll(existing);
        }
        for (SubmitFeedbackRequest.AnswerRequest answerReq : reponses) {
            String valeur = answerReq.valeur();
            if (valeur == null || valeur.isBlank()) {
                continue;
            }
            QuestionFeedback question = questionFeedbackRepository.findById(answerReq.questionId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Question not found: " + answerReq.questionId()
                    ));
            ReponseFeedback reponse = new ReponseFeedback();
            if (question.getType() == TypeQuestion.NOTE) {
                valeur = normalizeNoteValue(valeur);
            }
            reponse.setValeur(valeur);
            reponse.setQuestionFeedback(question);
            reponse.setFeedbackId(feedbackId);
            reponseFeedbackRepository.save(reponse);
        }
    }

    private Optional<Feedback> findActiveFeedback(Long utilisateurId, Long codingChallengeId) {
        return repository.findByUtilisateurIdAndCodingChallengeIdAndSupprimeFalse(
                utilisateurId,
                codingChallengeId
        );
    }

    private static boolean isSubmitted(Feedback feedback) {
        return feedback.getStatutFeedback() == StatutFeedback.SOUMIS;
    }

    private void markNotificationRead(Long utilisateurId, Long codingChallengeId) {
        notificationRepository
                .findByUtilisateurIdAndCodingChallengeId(utilisateurId, codingChallengeId)
                .ifPresent(notification -> {
                    if (notification.getStatut() != StatutNotification.LUE) {
                        notification.setStatut(StatutNotification.LUE);
                        notificationRepository.save(notification);
                    }
                });
    }

    private Feedback createDraftFeedback(Utilisateur utilisateur, CodingChallenge challenge) {
        Feedback feedback = new Feedback();
        feedback.setUtilisateur(utilisateur);
        feedback.setCodingChallenge(challenge);
        feedback.setChallengeTitre(challenge.getTitre());
        feedback.setChallengeTag(challenge.getTag());
        feedback.setChallengeDescription(challenge.getDescription());
        feedback.setStatutFeedback(StatutFeedback.EN_COURS);
        feedback.setCreatedAt(ZonedDateTime.now());
        return repository.save(feedback);
    }

    private FeedbackFormResponse buildFormResponse(
            CodingChallenge challenge,
            List<QuestionFeedback> questions,
            Feedback feedback,
            boolean alreadySubmitted
    ) {
        List<FeedbackDraftAnswerResponse> draftAnswers = feedback == null
                ? List.of()
                : reponseFeedbackRepository.findByFeedbackId(feedback.getId()).stream()
                        .map(r -> new FeedbackDraftAnswerResponse(
                                r.getQuestionFeedback().getId(),
                                r.getValeur()
                        ))
                        .toList();
        return new FeedbackFormResponse(
                codingChallengeMapper.toCodingChallengeDto(challenge),
                questions.stream()
                        .map(questionFeedbackMapper::toQuestionFeedbackResponse)
                        .toList(),
                alreadySubmitted,
                feedback == null ? null : feedback.getId(),
                feedback == null ? null : feedback.getNoteGlobale(),
                feedback == null ? null : feedback.getCommentaire(),
                draftAnswers
        );
    }

    private void collecterErreursQuestionsObligatoires(List<ErreurValidation> erreurs, SubmitFeedbackRequest request) {
        List<QuestionFeedback> mandatory = questionFeedbackRepository.findByObligatoire(true).stream()
                .filter(q -> !q.isSupprime())
                .toList();
        Set<Long> answeredIds = request.reponses() == null ? Set.of()
                : request.reponses().stream()
                .filter(r -> r.valeur() != null && !r.valeur().isBlank())
                .map(SubmitFeedbackRequest.AnswerRequest::questionId)
                .collect(Collectors.toSet());
        for (QuestionFeedback q : mandatory) {
            if (!answeredIds.contains(q.getId())) {
                integrationLogService.logEvent(
                        TypeLog.FEEDBACK,
                        StatutLog.ERREUR,
                        "Réponse obligatoire manquante : " + q.getLibelle(),
                        request.codingChallengeId()
                );
                erreurs.add(new ErreurValidation(
                        "reponses",
                        q.getId(),
                        "REPONSE_OBLIGATOIRE_MANQUANTE",
                        "Réponse obligatoire manquante pour : \"" + q.getLibelle() + "\""
                ));
            }
        }
    }

    private void collecterErreursFormatsReponses(List<ErreurValidation> erreurs, SubmitFeedbackRequest request) {
        if (request.reponses() == null) {
            return;
        }
        for (SubmitFeedbackRequest.AnswerRequest answerReq : request.reponses()) {
            String valeur = answerReq.valeur();
            if (valeur == null || valeur.isBlank()) {
                continue;
            }
            QuestionFeedback question = questionFeedbackRepository.findById(answerReq.questionId())
                    .orElse(null);
            if (question == null) {
                erreurs.add(new ErreurValidation(
                        "reponses",
                        answerReq.questionId(),
                        "QUESTION_INTROUVABLE",
                        "Question introuvable : id=" + answerReq.questionId()
                ));
                continue;
            }
            if (question.getType() == TypeQuestion.NOTE) {
                collecterErreurNote(erreurs, question, valeur.trim(), answerReq.questionId(), request.codingChallengeId());
            } else if (question.getType() == TypeQuestion.CHOIX) {
                collecterErreurChoix(erreurs, question, valeur.trim(), answerReq.questionId(), request.codingChallengeId());
            }
        }
    }

    private void collecterErreurNote(
            List<ErreurValidation> erreurs,
            QuestionFeedback question,
            String valeur,
            Long questionId,
            Long challengeId
    ) {
        String normalized = normalizeNoteValue(valeur);
        float parsed;
        try {
            parsed = Float.parseFloat(normalized);
        } catch (NumberFormatException ex) {
            integrationLogService.logEvent(
                    TypeLog.FEEDBACK,
                    StatutLog.ERREUR,
                    "Réponse NOTE invalide pour : " + question.getLibelle(),
                    challengeId
            );
            erreurs.add(new ErreurValidation(
                    "reponses",
                    questionId,
                    "NOTE_INVALIDE",
                    "La réponse à \"" + question.getLibelle() + "\" doit être un nombre (ex. 3.5)"
            ));
            return;
        }
        if (Float.isNaN(parsed) || Float.isInfinite(parsed) || parsed < 0f || parsed > 5f) {
            erreurs.add(new ErreurValidation(
                    "reponses",
                    questionId,
                    "NOTE_HORS_PLAGE",
                    "La réponse à \"" + question.getLibelle() + "\" doit être un nombre entre 0 et 5"
            ));
        }
    }

    private void collecterErreurChoix(
            List<ErreurValidation> erreurs,
            QuestionFeedback question,
            String valeur,
            Long questionId,
            Long challengeId
    ) {
        List<String> options = question.getChoix() == null ? List.of() : question.getChoix();
        if (options.isEmpty() || options.stream().noneMatch(valeur::equals)) {
            integrationLogService.logEvent(
                    TypeLog.FEEDBACK,
                    StatutLog.ERREUR,
                    "Réponse CHOIX invalide pour : " + question.getLibelle(),
                    challengeId
            );
            erreurs.add(new ErreurValidation(
                    "reponses",
                    questionId,
                    "CHOIX_INVALIDE",
                    "La réponse à \"" + question.getLibelle() + "\" doit être l'une des options proposées"
            ));
        }
    }

    /** Accepts values like "5." or "3." as 5 / 3 while typing or submitting. */
    private static String normalizeNoteValue(String valeur) {
        String trimmed = valeur.trim();
        if (trimmed.endsWith(".") && trimmed.length() > 1) {
            return trimmed.substring(0, trimmed.length() - 1);
        }
        if (trimmed.startsWith(".") && trimmed.length() > 1) {
            return "0" + trimmed;
        }
        return trimmed;
    }

    @Override
    public FeedbackFormResponse getFeedbackForm(Long codingChallengeId, Long utilisateurId) {
        CodingChallenge challenge = codingChallengeRepository.findById(codingChallengeId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Coding challenge not found: " + codingChallengeId
                ));
        if (challenge.isSupprime() && !canReadAllFeedbacks()) {
            throw new ResponseStatusException(HttpStatus.GONE, "This coding challenge has been archived");
        }

        List<QuestionFeedback> questions = questionFeedbackRepository.findBySupprimeFalse();
        Optional<Feedback> existing = findActiveFeedback(utilisateurId, codingChallengeId);

        if (existing.isPresent() && isSubmitted(existing.get())) {
            return buildFormResponse(challenge, questions, existing.get(), true);
        }

        assertFeedbackRecipient(utilisateurId, codingChallengeId);

        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Utilisateur introuvable : id=" + utilisateurId
                ));
        Feedback draft = existing.orElseGet(() -> createDraftFeedback(utilisateur, challenge));
        markNotificationRead(utilisateurId, codingChallengeId);
        integrationLogService.logEvent(
                TypeLog.FEEDBACK,
                StatutLog.INFO,
                "Brouillon feedback ouvert — user=" + utilisateurId + " challenge=" + codingChallengeId,
                codingChallengeId
        );
        return buildFormResponse(challenge, questions, draft, false);
    }

    @Override
    @Transactional(readOnly = true)
    public FeedbackResponse getFeedback(Long feedbackId, Long currentUserId) {
        Feedback feedback = findFeedbackOrThrow(feedbackId);
        assertFeedbackAccess(feedback, currentUserId);
        return mapper.toResponseDto(feedback);
    }

    @Override
    @Transactional(readOnly = true)
    public FeedbackDetailsResponse getFeedbackDetails(Long feedbackId, Long currentUserId) {
        Feedback feedback = findFeedbackOrThrow(feedbackId);
        assertFeedbackAccess(feedback, currentUserId);
        List<com.stage.backend.dto.reponsefeedback.ReponseFeedbackResponse> reponses =
                reponseFeedbackRepository.findByFeedbackId(feedbackId).stream()
                        .map(reponseFeedbackMapper::toReponseFeedbackResponse)
                        .toList();
        return new FeedbackDetailsResponse(mapper.toResponseDto(feedback), reponses);
    }

    private Feedback findFeedbackOrThrow(Long feedbackId) {
        return repository.findById(feedbackId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Feedback with id: " + feedbackId + " was not found"
                ));
    }

    private void assertFeedbackAccess(Feedback feedback, Long currentUserId) {
        if (canReadAllFeedbacks()) {
            return;
        }
        if (!feedback.getUtilisateur().getId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
    }

    private void assertFeedbackRecipient(Long utilisateurId, Long codingChallengeId) {
        if (canReadAllFeedbacks()) {
            return;
        }
        boolean hasNotification = notificationRepository
                .findByUtilisateurIdAndCodingChallengeId(utilisateurId, codingChallengeId)
                .filter(notification -> !notification.isSupprime())
                .isPresent();
        if (!hasNotification) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Ce formulaire de feedback ne vous est pas destiné"
            );
        }
    }

    private boolean canReadAllFeedbacks() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN_CODING_CHALLENGE")
                        || a.getAuthority().equals("ROLE_MANAGER_RH")
                        || a.getAuthority().equals("ROLE_ADMIN_CODEPULSE"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeedbackResponse> getAllFeedbacks() {
        return repository.findAll().stream().map(mapper::toResponseDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FeedbackResponse> getFeedbacksPage(int page, int size) {
        // Admins keep full history including feedbacks of archived challenges.
        return repository.findBySupprimeFalse(PageRequest.of(page, size)).map(mapper::toResponseDto);
    }

    @Override
    public Page<FeedbackResponse> searchFeedbacks(
            String keyword, StatutFeedback statut, String tag, int page, int size
    ) {
        String normalized = keyword == null ? "" : keyword.trim();
        String normalizedTag = tag == null ? "" : tag.trim();
        return repository.search(normalized, statut, normalizedTag, PageRequest.of(page, size))
                .map(mapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeedbackResponse> getFeedbacksByStatut(StatutFeedback statutFeedback) {
        return repository.findByStatutFeedback(statutFeedback).stream().map(mapper::toResponseDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeedbackResponse> getFeedbacksByNoteGlobale(Float noteGlobale) {
        return repository.findByNoteGlobale(noteGlobale).stream().map(mapper::toResponseDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeedbackResponse> getFeedbacksByCommentaire(String commentaire) {
        return repository.findByCommentaireContainingIgnoreCase(commentaire).stream().map(mapper::toResponseDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeedbackResponse> getFeedbacksByUtilisateur(Long utilisateurId) {
        return repository.findByUtilisateurId(utilisateurId).stream().map(mapper::toResponseDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FeedbackResponse> getFeedbacksByUtilisateurPage(Long utilisateurId, int page, int size) {
        return repository.findByUtilisateurIdAndSupprimeFalse(utilisateurId, PageRequest.of(page, size))
                .map(mapper::toResponseDto);
    }

    @Override
    public Page<FeedbackResponse> searchFeedbacksByUtilisateur(
            Long utilisateurId, String keyword, StatutFeedback statut, String tag, int page, int size
    ) {
        String normalized = keyword == null ? "" : keyword.trim();
        String normalizedTag = tag == null ? "" : tag.trim();
        return repository
                .searchByUtilisateur(utilisateurId, normalized, statut, normalizedTag, PageRequest.of(page, size))
                .map(mapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeedbackResponse> getFeedbacksByCreatedAt(ZonedDateTime createdAt) {
        return repository.findByCreatedAt(createdAt).stream().map(mapper::toResponseDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countFeedbacks() {
        return repository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public long countFeedbacksByStatut(StatutFeedback statutFeedback) {
        return repository.countByStatutFeedback(statutFeedback);
    }

    @Override
    @Transactional(readOnly = true)
    public Float getAverageNoteGlobale() {
        return repository.getAverageNoteGlobale();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long feedbackId) {
        return repository.existsById(feedbackId);
    }
}
