package com.stage.backend.service.analytics;

import com.stage.backend.dto.analytics.*;
import com.stage.backend.entity.CodingChallenge;
import com.stage.backend.entity.Feedback;
import com.stage.backend.entity.ReponseFeedback;
import com.stage.backend.enums.Role;
import com.stage.backend.enums.StatutDemandeReinit;
import com.stage.backend.enums.StatutFeedback;
import com.stage.backend.enums.StatutLog;
import com.stage.backend.enums.StatutNotification;
import com.stage.backend.repository.CodingChallengeRepository;
import com.stage.backend.repository.DemandeReinitialisationRepository;
import com.stage.backend.repository.FeedbackRepository;
import com.stage.backend.repository.IntegrationLogRepository;
import com.stage.backend.repository.NotificationRepository;
import com.stage.backend.repository.QuestionFeedbackRepository;
import com.stage.backend.repository.ReponseFeedbackRepository;
import com.stage.backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsServiceImp implements AnalyticsService {

    private final FeedbackRepository feedbackRepository;
    private final CodingChallengeRepository codingChallengeRepository;
    private final QuestionFeedbackRepository questionFeedbackRepository;
    private final ReponseFeedbackRepository reponseFeedbackRepository;
    private final NotificationRepository notificationRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final DemandeReinitialisationRepository demandeReinitialisationRepository;
    private final IntegrationLogRepository integrationLogRepository;
    private final Environment environment;

    @Override
    public AverageScoreByTagResponse getAverageScoreByTag(String tag) {
        List<Feedback> feedbacks = submittedFeedbacks().stream()
                .filter(f -> f.getCodingChallenge() != null)
                .filter(f -> Objects.equals(normalize(f.getCodingChallenge().getTag()), normalize(tag)))
                .toList();
        return new AverageScoreByTagResponse(tag, averageScore(feedbacks));
    }

    @Override
    public List<AverageScoreByTagResponse> getAverageScoresByAllTags() {
        Map<String, List<Feedback>> byTag = submittedFeedbacks().stream()
                .filter(f -> f.getCodingChallenge() != null)
                .collect(Collectors.groupingBy(f -> {
                    String tag = f.getCodingChallenge().getTag();
                    return tag == null || tag.isBlank() ? "general" : tag;
                }));

        return byTag.entrySet().stream()
                .map(e -> new AverageScoreByTagResponse(e.getKey(), averageScore(e.getValue())))
                .sorted(Comparator.comparing(AverageScoreByTagResponse::tag))
                .toList();
    }

    @Override
    public CompletionRateResponse getCompletionRateByTag(String tag) {
        List<CodingChallenge> challenges = codingChallengeRepository.findBySupprimeFalse().stream()
                .filter(c -> Objects.equals(normalize(c.getTag()), normalize(tag)))
                .toList();
        long total = challenges.size();
        if (total == 0) {
            return new CompletionRateResponse(tag, 0.0);
        }
        long withFeedback = challenges.stream()
                .filter(c -> feedbackRepository.existsByCodingChallengeId(c.getId()))
                .count();
        return new CompletionRateResponse(tag, (double) withFeedback / total);
    }

    @Override
    public FeedbackParticipationResponse getFeedbackParticipationRate() {
        long totalChallenges = codingChallengeRepository.countBySupprimeFalse();
        long submitted = feedbackRepository.countByStatutFeedbackAndSupprimeFalse(StatutFeedback.SOUMIS);
        if (submitted == 0) {
            submitted = feedbackRepository.countBySupprimeFalse();
        }
        double rate = totalChallenges == 0 ? 0.0 : (double) submitted / totalChallenges * 100.0;
        return new FeedbackParticipationResponse(rate, totalChallenges, submitted);
    }

    @Override
    public ChallengeStatisticsResponse getChallengeStatistics(Long challengeId) {
        CodingChallenge challenge = codingChallengeRepository.findById(challengeId)
                .orElseThrow(() -> new IllegalArgumentException("Challenge not found: " + challengeId));
        List<Feedback> feedbacks = submittedFeedbacks().stream()
                .filter(f -> f.getCodingChallenge() != null && challengeId.equals(f.getCodingChallenge().getId()))
                .toList();
        return new ChallengeStatisticsResponse(
                challenge.getId(),
                challenge.getTitre(),
                averageScore(feedbacks),
                (long) feedbacks.size()
        );
    }

    @Override
    public List<ChallengeStatisticsResponse> getAllChallengeStatistics() {
        return codingChallengeRepository.findBySupprimeFalse().stream()
                .map(c -> getChallengeStatistics(c.getId()))
                .sorted(Comparator.comparing(ChallengeStatisticsResponse::feedbackCount).reversed())
                .toList();
    }

    @Override
    public Page<ChallengeStatisticsResponse> getChallengeStatisticsPage(int page, int size) {
        return codingChallengeRepository
                .findBySupprimeFalse(PageRequest.of(Math.max(0, page), Math.max(1, size)))
                .map(c -> getChallengeStatistics(c.getId()));
    }

    @Override
    public PeriodExportResponse exportDataForPeriod(ZonedDateTime startDate, ZonedDateTime endDate, String format) {
        String body = submittedFeedbacks().stream()
                .filter(f -> f.getCreatedAt() != null)
                .filter(f -> !f.getCreatedAt().isBefore(startDate) && !f.getCreatedAt().isAfter(endDate))
                .map(f -> String.join(",",
                        String.valueOf(f.getId()),
                        String.valueOf(f.getUtilisateur() != null ? f.getUtilisateur().getId() : ""),
                        String.valueOf(f.getCodingChallenge() != null ? f.getCodingChallenge().getId() : ""),
                        String.valueOf(f.getNoteGlobale()),
                        f.getStatutFeedback() != null ? f.getStatutFeedback().name() : "",
                        f.getCreatedAt().toString()
                ))
                .collect(Collectors.joining("\n", "id,userId,challengeId,note,statut,createdAt\n", "\n"));
        String exportFormat = format == null || format.isBlank() ? "csv" : format;
        return new PeriodExportResponse(startDate, endDate, exportFormat, body.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public List<ChallengeRankingResponse> getTopChallenges(int limit) {
        return rankChallenges(limit, true);
    }

    @Override
    public List<ChallengeRankingResponse> getBottomChallenges(int limit) {
        return rankChallenges(limit, false);
    }

    @Override
    public List<MandatoryQuestionResponseRateResponse> getMandatoryQuestionResponseRates() {
        long submittedCount = Math.max(1, feedbackRepository.countByStatutFeedbackAndSupprimeFalse(StatutFeedback.SOUMIS));
        if (feedbackRepository.countByStatutFeedbackAndSupprimeFalse(StatutFeedback.SOUMIS) == 0) {
            submittedCount = Math.max(1, feedbackRepository.countBySupprimeFalse());
        }
        final long denominator = submittedCount;

        return questionFeedbackRepository.findByObligatoire(true).stream()
                .filter(q -> !q.isSupprime())
                .map(q -> {
                    long answered = reponseFeedbackRepository.findByQuestionFeedbackId(q.getId()).stream()
                            .map(ReponseFeedback::getValeur)
                            .filter(v -> v != null && !v.isBlank())
                            .count();
                    return new MandatoryQuestionResponseRateResponse(
                            q.getId(),
                            q.getLibelle(),
                            (double) answered / denominator
                    );
                })
                .toList();
    }

    @Override
    public UserDashboardKpiResponse getUserDashboardKpis(Long utilisateurId) {
        long total = notificationRepository.countByUtilisateurIdAndSupprimeFalse(utilisateurId);
        long pending = notificationRepository.countByUtilisateurIdAndStatutAndSupprimeFalse(
                utilisateurId,
                StatutNotification.ENVOYEE
        ) + notificationRepository.countByUtilisateurIdAndStatutAndSupprimeFalse(
                utilisateurId,
                StatutNotification.EN_ATTENTE
        );
        long submitted = feedbackRepository.countByUtilisateurIdAndSupprimeFalse(utilisateurId);
        return new UserDashboardKpiResponse(total, pending, submitted);
    }

    @Override
    public ChallengeAdminDashboardKpiResponse getChallengeAdminDashboardKpis() {
        long actifs = codingChallengeRepository.countBySupprimeFalse();
        long archives = codingChallengeRepository.countBySupprimeTrue();
        long notifTotal = notificationRepository.countBySupprimeFalse();
        long notifPending = notificationRepository.countByStatutAndSupprimeFalse(StatutNotification.ENVOYEE)
                + notificationRepository.countByStatutAndSupprimeFalse(StatutNotification.EN_ATTENTE);
        long feedbacks = feedbackRepository.countBySupprimeFalse();
        double note = averageScore(submittedFeedbacks());
        return new ChallengeAdminDashboardKpiResponse(
                actifs,
                archives,
                notifTotal,
                notifPending,
                feedbacks,
                note
        );
    }

    @Override
    public ManagerDashboardKpiResponse getManagerDashboardKpis() {
        FeedbackParticipationResponse participation = getFeedbackParticipationRate();
        double note = averageScore(submittedFeedbacks());
        long pending = notificationRepository.countByStatutAndSupprimeFalse(StatutNotification.ENVOYEE)
                + notificationRepository.countByStatutAndSupprimeFalse(StatutNotification.EN_ATTENTE);
        long tags = getAverageScoresByAllTags().size();
        return new ManagerDashboardKpiResponse(
                participation.participationRate(),
                participation.totalChallenges(),
                participation.submittedFeedbacks(),
                note,
                pending,
                tags
        );
    }

    @Override
    public AppAdminDashboardKpiResponse getAppAdminDashboardKpis() {
        long staff = utilisateurRepository.countByRoleAndSupprimeFalse(Role.ADMIN_CODEPULSE)
                + utilisateurRepository.countByRoleAndSupprimeFalse(Role.ADMIN_CODING_CHALLENGE)
                + utilisateurRepository.countByRoleAndSupprimeFalse(Role.MANAGER_RH);
        long candidats = utilisateurRepository.countByRoleAndSupprimeFalse(Role.USER);
        long questions = questionFeedbackRepository.countBySupprimeFalse();
        long reinit = demandeReinitialisationRepository.countByStatut(StatutDemandeReinit.EN_ATTENTE);
        long errors = integrationLogRepository.countByStatut(StatutLog.ERREUR)
                + integrationLogRepository.countByStatut(StatutLog.CRITIQUE);
        long challenges = codingChallengeRepository.countBySupprimeFalse();
        long feedbacks = feedbackRepository.countByStatutFeedbackAndSupprimeFalse(StatutFeedback.SOUMIS);
        if (feedbacks == 0) {
            feedbacks = feedbackRepository.countBySupprimeFalse();
        }
        boolean kafka = environment.getProperty("codepulse.kafka.enabled", Boolean.class, false);
        boolean notification = environment.getProperty("codepulse.notification.enabled", Boolean.class, false);
        return new AppAdminDashboardKpiResponse(
                staff,
                candidats,
                questions,
                reinit,
                errors,
                challenges,
                feedbacks,
                kafka,
                notification
        );
    }

    private List<ChallengeRankingResponse> rankChallenges(int limit, boolean top) {
        int safeLimit = Math.max(1, limit);
        Comparator<ChallengeStatisticsResponse> comparator =
                Comparator.comparing(s -> s.averageScore() == null ? -1.0 : s.averageScore());
        if (top) {
            comparator = comparator.reversed();
        }

        return getAllChallengeStatistics().stream()
                .filter(s -> s.feedbackCount() != null && s.feedbackCount() > 0)
                .sorted(comparator)
                .limit(safeLimit)
                .map(s -> new ChallengeRankingResponse(
                        s.challengeId(),
                        s.titre(),
                        s.averageScore(),
                        top ? "TOP" : "BOTTOM"
                ))
                .toList();
    }

    private List<Feedback> submittedFeedbacks() {
        List<Feedback> submitted = feedbackRepository.findByStatutFeedback(StatutFeedback.SOUMIS);
        if (!submitted.isEmpty()) {
            return submitted.stream().filter(f -> !f.isSupprime()).toList();
        }
        return feedbackRepository.findAll().stream().filter(f -> !f.isSupprime()).toList();
    }

    private Double averageScore(List<Feedback> feedbacks) {
        return feedbacks.stream()
                .map(Feedback::getNoteGlobale)
                .filter(Objects::nonNull)
                .mapToDouble(Float::doubleValue)
                .average()
                .orElse(0.0);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
