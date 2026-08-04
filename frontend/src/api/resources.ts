import client from './client';
import type {
  AppAdminDashboardKpi,
  CodingChallengeDto,
  ChallengeAdminDashboardKpi,
  DemandeReinitialisationDto,
  FeedbackFormResponse,
  FeedbackParticipation,
  FeedbackResponse,
  AverageScoreByTag,
  ChallengeRanking,
  ChallengeStatistics,
  IntegrationLogDto,
  ManagerDashboardKpi,
  MeResponse,
  NotificationDto,
  PageResponse,
  QuestionFeedbackDto,
  SubmitFeedbackRequest,
  UserDashboardKpi,
  UtilisateurDto,
} from './types';

export async function getNotificationsByUserPage(
  utilisateurId: number,
  page: number,
  size: number,
  q?: string,
  statut?: string
) {
  const { data } = await client.get<PageResponse<NotificationDto>>(
    '/notifications/get-notifications-by-utilisateur-pages/page',
    { params: { utilisateurId, page, size, ...(q ? { q } : {}), ...(statut ? { statut } : {}) } }
  );
  return data;
}

export async function getNotificationsPage(page: number, size: number, statut?: string, q?: string) {
  const { data } = await client.get<PageResponse<NotificationDto>>(
    '/notifications/get-notifications-pages/page',
    { params: { page, size, ...(statut ? { statut } : {}), ...(q ? { q } : {}) } }
  );
  return data;
}

export async function updateNotificationStatut(id: number, statut: string) {
  const { data } = await client.patch<boolean>(`/notifications/update-statut/${id}/statut`, null, {
    params: { statut },
  });
  return data;
}

export async function getFeedbackForm(challengeId: number) {
  const { data } = await client.get<FeedbackFormResponse>('/feedbacks/form', {
    params: { challengeId },
  });
  return data;
}

export async function submitFeedback(body: SubmitFeedbackRequest) {
  const { data } = await client.post<FeedbackResponse>('/feedbacks/submit', body);
  return data;
}

export async function getFeedbacksByUserPage(
  utilisateurId: number,
  page: number,
  size: number,
  q?: string,
  statut?: string
) {
  const { data } = await client.get<PageResponse<FeedbackResponse>>(
    '/feedbacks/get-feedback-by-utilisateur-pages/page',
    { params: { utilisateurId, page, size, ...(q ? { q } : {}), ...(statut ? { statut } : {}) } }
  );
  return data;
}

export async function getFeedbacksPage(page: number, size: number, q?: string, statut?: string) {
  const { data } = await client.get<PageResponse<FeedbackResponse>>('/feedbacks/get-feedback-pages/page', {
    params: { page, size, ...(q ? { q } : {}), ...(statut ? { statut } : {}) },
  });
  return data;
}

export async function getFeedbackDetails(id: number) {
  const { data } = await client.get(`/feedbacks/details/${id}`);
  return data;
}

export async function getUsersPage(page: number, size: number, q?: string, role?: string) {
  const { data } = await client.get<PageResponse<UtilisateurDto>>('/utilisateurs/get-users-pages/page', {
    params: { page, size, ...(q ? { q } : {}), ...(role ? { role } : {}) },
  });
  return data;
}

export async function createUser(body: {
  nom: string;
  prenom: string;
  email: string;
  rawPassword: string;
  role: string;
}) {
  const { data } = await client.post<UtilisateurDto>('/utilisateurs/add-user', body);
  return data;
}

export async function deleteUser(id: number) {
  await client.delete(`/utilisateurs/delete-user/${id}`);
}

export async function promoteRole(id: number, role: string) {
  const { data } = await client.patch<UtilisateurDto>(`/utilisateurs/promote-role/${id}`, { role });
  return data;
}

export async function getMe() {
  const { data } = await client.get<MeResponse>('/api/me');
  return data;
}

export async function getChallengesPage(page: number, size: number, q?: string, tag?: string) {
  const { data } = await client.get<PageResponse<CodingChallengeDto>>(
    '/coding-challenges/get-coding-challenges-pages/page',
    { params: { page, size, ...(q ? { q } : {}), ...(tag ? { tag } : {}) } }
  );
  return data;
}

export async function syncChallenges() {
  const { data } = await client.post<{ published: number }>('/coding-challenges/synchroniser', {});
  return data;
}

export async function deleteChallenge(id: number) {
  const { data } = await client.delete<boolean>(`/coding-challenges/delete-coding-challenge/${id}`);
  return data;
}

export async function getLogsPage(
  page: number,
  size: number,
  q?: string,
  type?: string,
  statut?: string
) {
  const { data } = await client.get<PageResponse<IntegrationLogDto>>(
    '/integration-logs/get-integration-logs-pages/page',
    { params: { page, size, ...(q ? { q } : {}), ...(type ? { type } : {}), ...(statut ? { statut } : {}) } }
  );
  return data;
}

export async function getQuestionsPage(page: number, size: number, q?: string, type?: string) {
  const { data } = await client.get<PageResponse<QuestionFeedbackDto>>(
    '/questions-feedback/get-questions-pages/page',
    { params: { page, size, ...(q ? { q } : {}), ...(type ? { type } : {}) } }
  );
  return data;
}

export async function addQuestion(body: {
  libelle: string;
  type: string;
  obligatoire: boolean;
  choix?: string[];
}) {
  const { data } = await client.post<QuestionFeedbackDto>('/questions-feedback/add-question', body);
  return data;
}

export async function deleteQuestion(id: number) {
  await client.delete(`/questions-feedback/delete-question/${id}`);
}

export async function getDemandesPage(page: number, size: number, statut?: string, q?: string) {
  const { data } = await client.get<PageResponse<DemandeReinitialisationDto>>('/demandes-reinit/page', {
    params: { page, size, ...(statut ? { statut } : {}), ...(q ? { q } : {}) },
  });
  return data;
}

export async function sendResetLink(id: number) {
  const { data } = await client.post<DemandeReinitialisationDto>(`/demandes-reinit/${id}/send-link`, {});
  return data;
}

export async function setTemporaryPassword(id: number, temporaryPassword: string) {
  const { data } = await client.post<DemandeReinitialisationDto>(
    `/demandes-reinit/${id}/temporary-password`,
    { temporaryPassword }
  );
  return data;
}

export async function rejectDemande(id: number) {
  const { data } = await client.post<DemandeReinitialisationDto>(`/demandes-reinit/${id}/reject`, {});
  return data;
}

export async function getParticipation() {
  const { data } = await client.get<FeedbackParticipation>('/analytics/feedback-participation');
  return data;
}

export async function getScoresByTags() {
  const { data } = await client.get<AverageScoreByTag[]>('/analytics/average-scores-by-tags');
  return data;
}

export async function getTopChallenges(limit = 5) {
  const { data } = await client.get<ChallengeRanking[]>('/analytics/top-challenges', {
    params: { limit },
  });
  return data;
}

export async function getChallengeStatistics() {
  const { data } = await client.get<ChallengeStatistics[]>('/analytics/challenge-statistics');
  return data;
}

export async function getChallengeStatisticsPage(page: number, size: number, q?: string) {
  const { data } = await client.get<PageResponse<ChallengeStatistics>>(
    '/analytics/challenge-statistics/page',
    { params: { page, size, ...(q ? { q } : {}) } }
  );
  return data;
}

export async function getUserDashboardKpis() {
  const { data } = await client.get<UserDashboardKpi>('/analytics/dashboard/user');
  return data;
}

export async function getChallengeAdminDashboardKpis() {
  const { data } = await client.get<ChallengeAdminDashboardKpi>('/analytics/dashboard/challenge-admin');
  return data;
}

export async function getManagerDashboardKpis() {
  const { data } = await client.get<ManagerDashboardKpi>('/analytics/dashboard/manager');
  return data;
}

export async function getAppAdminDashboardKpis() {
  const { data } = await client.get<AppAdminDashboardKpi>('/analytics/dashboard/app-admin');
  return data;
}
