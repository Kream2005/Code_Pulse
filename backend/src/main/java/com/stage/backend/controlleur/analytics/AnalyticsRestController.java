package com.stage.backend.controlleur.analytics;

import com.stage.backend.dto.analytics.*;
import com.stage.backend.enums.StatutLog;
import com.stage.backend.enums.TypeLog;
import com.stage.backend.security.JwtUtils;
import com.stage.backend.security.SecurityRoles;
import com.stage.backend.service.analytics.AnalyticsService;
import com.stage.backend.service.integrationlog.IntegrationLogService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.List;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
@Validated
public class AnalyticsRestController {

    private final AnalyticsService analyticsService;
    private final IntegrationLogService integrationLogService;

    @GetMapping("/dashboard/user")
    @PreAuthorize(SecurityRoles.USER)
    public ResponseEntity<UserDashboardKpiResponse> getUserDashboardKpis() {
        logAccess("dashboard-user");
        return ResponseEntity.ok(analyticsService.getUserDashboardKpis(JwtUtils.getCurrentUserId()));
    }

    @GetMapping("/dashboard/challenge-admin")
    @PreAuthorize(SecurityRoles.ADMIN_CHALLENGE)
    public ResponseEntity<ChallengeAdminDashboardKpiResponse> getChallengeAdminDashboardKpis() {
        logAccess("dashboard-challenge-admin");
        return ResponseEntity.ok(analyticsService.getChallengeAdminDashboardKpis());
    }

    @GetMapping("/dashboard/manager")
    @PreAuthorize(SecurityRoles.ANALYTICS)
    public ResponseEntity<ManagerDashboardKpiResponse> getManagerDashboardKpis() {
        logAccess("dashboard-manager");
        return ResponseEntity.ok(analyticsService.getManagerDashboardKpis());
    }

    @GetMapping("/dashboard/app-admin")
    @PreAuthorize(SecurityRoles.ADMIN_CODEPULSE)
    public ResponseEntity<AppAdminDashboardKpiResponse> getAppAdminDashboardKpis() {
        logAccess("dashboard-app-admin");
        return ResponseEntity.ok(analyticsService.getAppAdminDashboardKpis());
    }

    @GetMapping("/average-score-by-tag")
    @PreAuthorize(SecurityRoles.ANALYTICS)
    public ResponseEntity<AverageScoreByTagResponse> getAverageScoreByTag(
            @RequestParam @NotBlank String tag
    ) {
        logAccess("average-score-by-tag");
        return ResponseEntity.ok(analyticsService.getAverageScoreByTag(tag));
    }

    @GetMapping("/average-scores-by-tags")
    @PreAuthorize(SecurityRoles.ANALYTICS)
    public ResponseEntity<List<AverageScoreByTagResponse>> getAverageScoresByAllTags() {
        logAccess("average-scores-by-tags");
        return ResponseEntity.ok(analyticsService.getAverageScoresByAllTags());
    }

    @GetMapping("/completion-rate-by-tag")
    @PreAuthorize(SecurityRoles.ANALYTICS)
    public ResponseEntity<CompletionRateResponse> getCompletionRateByTag(
            @RequestParam @NotBlank String tag
    ) {
        logAccess("completion-rate-by-tag");
        return ResponseEntity.ok(analyticsService.getCompletionRateByTag(tag));
    }

    @GetMapping("/feedback-participation")
    @PreAuthorize(SecurityRoles.ANALYTICS)
    public ResponseEntity<FeedbackParticipationResponse> getFeedbackParticipationRate() {
        logAccess("feedback-participation");
        return ResponseEntity.ok(analyticsService.getFeedbackParticipationRate());
    }

    @GetMapping("/challenge-statistics/page")
    @PreAuthorize(SecurityRoles.READ_FEEDBACKS)
    public ResponseEntity<Page<ChallengeStatisticsResponse>> getChallengeStatisticsPage(
            @RequestParam @Min(0) int page,
            @RequestParam @Min(1) int size,
            @RequestParam(required = false) String q
    ) {
        logAccess("challenge-statistics-page");
        return ResponseEntity.ok(analyticsService.searchChallengeStatistics(q, page, size));
    }

    @GetMapping("/tag-statistics/page")
    @PreAuthorize(SecurityRoles.ANALYTICS)
    public ResponseEntity<Page<TagStatisticsResponse>> getTagStatisticsPage(
            @RequestParam @Min(0) int page,
            @RequestParam @Min(1) int size,
            @RequestParam(required = false) String q
    ) {
        logAccess("tag-statistics-page");
        return ResponseEntity.ok(analyticsService.searchTagStatistics(q, page, size));
    }

    @GetMapping("/lowest-scoring-tags")
    @PreAuthorize(SecurityRoles.ANALYTICS)
    public ResponseEntity<List<TagStatisticsResponse>> getLowestScoringTags(
            @RequestParam(defaultValue = "5") @Min(1) int limit
    ) {
        logAccess("lowest-scoring-tags");
        return ResponseEntity.ok(analyticsService.getLowestScoringTags(limit));
    }

    @GetMapping("/challenge-statistics/{challengeId}")
    @PreAuthorize(SecurityRoles.READ_FEEDBACKS)
    public ResponseEntity<ChallengeStatisticsResponse> getChallengeStatistics(
            @PathVariable @NotNull Long challengeId
    ) {
        logAccess("challenge-statistics");
        return ResponseEntity.ok(analyticsService.getChallengeStatistics(challengeId));
    }

    @GetMapping("/challenge-statistics")
    @PreAuthorize(SecurityRoles.READ_FEEDBACKS)
    public ResponseEntity<List<ChallengeStatisticsResponse>> getAllChallengeStatistics() {
        logAccess("challenge-statistics-all");
        return ResponseEntity.ok(analyticsService.getAllChallengeStatistics());
    }

    @GetMapping("/export")
    @PreAuthorize(SecurityRoles.ANALYTICS)
    public ResponseEntity<PeriodExportResponse> exportDataForPeriod(
            @RequestParam ZonedDateTime startDate,
            @RequestParam ZonedDateTime endDate,
            @RequestParam(defaultValue = "csv") String format
    ) {
        logAccess("export");
        return ResponseEntity.ok(analyticsService.exportDataForPeriod(startDate, endDate, format));
    }

    @GetMapping("/top-challenges")
    @PreAuthorize(SecurityRoles.ANALYTICS)
    public ResponseEntity<List<ChallengeRankingResponse>> getTopChallenges(
            @RequestParam(defaultValue = "10") @Min(1) int limit
    ) {
        logAccess("top-challenges");
        return ResponseEntity.ok(analyticsService.getTopChallenges(limit));
    }

    @GetMapping("/bottom-challenges")
    @PreAuthorize(SecurityRoles.ANALYTICS)
    public ResponseEntity<List<ChallengeRankingResponse>> getBottomChallenges(
            @RequestParam(defaultValue = "10") @Min(1) int limit
    ) {
        logAccess("bottom-challenges");
        return ResponseEntity.ok(analyticsService.getBottomChallenges(limit));
    }

    @GetMapping("/mandatory-question-response-rates")
    @PreAuthorize(SecurityRoles.ANALYTICS)
    public ResponseEntity<List<MandatoryQuestionResponseRateResponse>> getMandatoryQuestionResponseRates() {
        logAccess("mandatory-question-response-rates");
        return ResponseEntity.ok(analyticsService.getMandatoryQuestionResponseRates());
    }

    private void logAccess(String endpoint) {
        integrationLogService.logEvent(
                TypeLog.EXPORT_DONNEES,
                StatutLog.INFO,
                "Analytics access: " + endpoint,
                null
        );
    }
}
