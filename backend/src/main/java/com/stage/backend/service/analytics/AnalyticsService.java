package com.stage.backend.service.analytics;

import com.stage.backend.dto.analytics.*;
import org.springframework.data.domain.Page;

import java.time.ZonedDateTime;
import java.util.List;

public interface AnalyticsService {

    AverageScoreByTagResponse getAverageScoreByTag(String tag);

    List<AverageScoreByTagResponse> getAverageScoresByAllTags();

    CompletionRateResponse getCompletionRateByTag(String tag);

    FeedbackParticipationResponse getFeedbackParticipationRate();

    ChallengeStatisticsResponse getChallengeStatistics(Long challengeId);

    List<ChallengeStatisticsResponse> getAllChallengeStatistics();

    Page<ChallengeStatisticsResponse> getChallengeStatisticsPage(int page, int size);

    PeriodExportResponse exportDataForPeriod(ZonedDateTime startDate, ZonedDateTime endDate, String format);

    List<ChallengeRankingResponse> getTopChallenges(int limit);

    List<ChallengeRankingResponse> getBottomChallenges(int limit);

    List<MandatoryQuestionResponseRateResponse> getMandatoryQuestionResponseRates();

    UserDashboardKpiResponse getUserDashboardKpis(Long utilisateurId);

    ChallengeAdminDashboardKpiResponse getChallengeAdminDashboardKpis();

    ManagerDashboardKpiResponse getManagerDashboardKpis();

    AppAdminDashboardKpiResponse getAppAdminDashboardKpis();
}
