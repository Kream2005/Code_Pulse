package com.stage.backend.service.feedback;
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
import com.stage.backend.service.integrationlog.IntegrationLogService;
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
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
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
        if (repository.existsByUtilisateurIdAndCodingChallengeId(utilisateurId, request.codingChallengeId())
                || repository.existsByCodingChallengeId(request.codingChallengeId())) {
            integrationLogService.logEvent(
                    TypeLog.FEEDBACK,
                    StatutLog.WARNING,
                    "Duplicate feedback submit for challenge " + request.codingChallengeId(),
                    request.codingChallengeId()
            );
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Feedback already submitted for this challenge"
            );
        }
        CodingChallenge challenge = codingChallengeRepository.findById(request.codingChallengeId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Coding challenge not found: " + request.codingChallengeId()
                ));
        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "User not found: " + utilisateurId
                ));
        if (request.statut() == StatutFeedback.SOUMIS) {
            validateMandatoryQuestions(request);
            validateAnswerFormats(request);
        }
        if (challenge.isSupprime()) {
            throw new ResponseStatusException(HttpStatus.GONE, "This coding challenge has been archived");
        }
        Feedback feedback = new Feedback();
        feedback.setUtilisateur(utilisateur);
        feedback.setCodingChallenge(challenge);
        feedback.setChallengeTitre(challenge.getTitre());
        feedback.setChallengeTag(challenge.getTag());
        feedback.setChallengeDescription(challenge.getDescription());
        feedback.setNoteGlobale(request.noteGlobale());
        feedback.setCommentaire(request.commentaire());
        feedback.setStatutFeedback(request.statut());
        feedback.setCreatedAt(ZonedDateTime.now());
        Feedback saved = repository.save(feedback);
        if (request.reponses() != null) {
            for (SubmitFeedbackRequest.AnswerRequest answerReq : request.reponses()) {
                QuestionFeedback question = questionFeedbackRepository.findById(answerReq.questionId())
                        .orElseThrow(() -> new EntityNotFoundException(
                                "Question not found: " + answerReq.questionId()
                        ));
                ReponseFeedback reponse = new ReponseFeedback();
                String valeur = answerReq.valeur();
                if (question.getType() == TypeQuestion.NOTE && valeur != null && !valeur.isBlank()) {
                    valeur = normalizeNoteValue(valeur);
                }
                reponse.setValeur(valeur);
                reponse.setQuestionFeedback(question);
                reponse.setFeedbackId(saved.getId());
                reponseFeedbackRepository.save(reponse);
            }
        }
        if (request.statut() == StatutFeedback.SOUMIS) {
            notificationRepository
                    .findByUtilisateurIdAndCodingChallengeId(utilisateurId, challenge.getId())
                    .ifPresent(n -> {
                        n.setStatut(StatutNotification.LUE);
                        notificationRepository.save(n);
                    });
        }
        integrationLogService.logEvent(
                TypeLog.FEEDBACK,
                StatutLog.SUCCES,
                "Feedback submitted by user " + utilisateurId + " for challenge " + request.codingChallengeId(),
                request.codingChallengeId()
        );
        log.info("Feedback saved — user={} challenge={} statut={}",
                utilisateurId, request.codingChallengeId(), request.statut());
        return mapper.toResponseDto(saved);
    }

    private void validateMandatoryQuestions(SubmitFeedbackRequest request) {
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
                        "Missing mandatory answer: " + q.getLibelle(),
                        request.codingChallengeId()
                );
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Missing answer for mandatory question: " + q.getLibelle()
                );
            }
        }
    }

    private void validateAnswerFormats(SubmitFeedbackRequest request) {
        if (request.reponses() == null) {
            return;
        }
        for (SubmitFeedbackRequest.AnswerRequest answerReq : request.reponses()) {
            String valeur = answerReq.valeur();
            if (valeur == null || valeur.isBlank()) {
                continue;
            }
            QuestionFeedback question = questionFeedbackRepository.findById(answerReq.questionId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Question not found: " + answerReq.questionId()
                    ));
            if (question.getType() == TypeQuestion.NOTE) {
                validateNoteAnswer(question, valeur.trim(), request.codingChallengeId());
            } else if (question.getType() == TypeQuestion.CHOIX) {
                validateChoixAnswer(question, valeur.trim(), request.codingChallengeId());
            }
        }
    }

    private void validateNoteAnswer(QuestionFeedback question, String valeur, Long challengeId) {
        String normalized = normalizeNoteValue(valeur);
        float parsed;
        try {
            parsed = Float.parseFloat(normalized);
        } catch (NumberFormatException ex) {
            integrationLogService.logEvent(
                    TypeLog.FEEDBACK,
                    StatutLog.ERREUR,
                    "Invalid NOTE answer for: " + question.getLibelle(),
                    challengeId
            );
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Answer for \"" + question.getLibelle() + "\" must be a number (e.g. 3.5)"
            );
        }
        if (Float.isNaN(parsed) || Float.isInfinite(parsed) || parsed < 0f || parsed > 5f) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Answer for \"" + question.getLibelle() + "\" must be a number between 0 and 5"
            );
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

    private void validateChoixAnswer(QuestionFeedback question, String valeur, Long challengeId) {
        List<String> options = question.getChoix() == null ? List.of() : question.getChoix();
        if (options.isEmpty() || options.stream().noneMatch(valeur::equals)) {
            integrationLogService.logEvent(
                    TypeLog.FEEDBACK,
                    StatutLog.ERREUR,
                    "Invalid CHOIX answer for: " + question.getLibelle(),
                    challengeId
            );
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Answer for \"" + question.getLibelle() + "\" must be one of the proposed choices"
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public FeedbackFormResponse getFeedbackForm(Long codingChallengeId, Long utilisateurId) {
        CodingChallenge challenge = codingChallengeRepository.findById(codingChallengeId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Coding challenge not found: " + codingChallengeId
                ));
        if (challenge.isSupprime() && !canReadAllFeedbacks()) {
            throw new ResponseStatusException(HttpStatus.GONE, "This coding challenge has been archived");
        }
        return new FeedbackFormResponse(
                codingChallengeMapper.toCodingChallengeDto(challenge),
                questionFeedbackRepository.findBySupprimeFalse().stream()
                        .map(questionFeedbackMapper::toQuestionFeedbackResponse)
                        .toList(),
                repository.existsByUtilisateurIdAndCodingChallengeId(utilisateurId, codingChallengeId)
                        || repository.existsByCodingChallengeId(codingChallengeId)
        );
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
