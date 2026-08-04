export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
}

export interface UtilisateurDto {
  id: number;
  nom: string;
  prenom: string;
  email: string;
  role: string;
}

export interface SetupAccountInfo {
  email: string;
  nom: string | null;
  prenom: string | null;
  userName: string | null;
}

export interface NotificationDto {
  id: number;
  dateEnvoi: string;
  statut: string;
  utilisateurId: number;
  codingChallengeId: number;
  challengeTitre?: string | null;
  challengeTag?: string | null;
  challengeDuree?: number | null;
  challengeDescription?: string | null;
}

export interface CodingChallengeDto {
  id: number;
  externalId: number;
  titre: string;
  description: string;
  tag: string;
  duree: number;
  codeUrl: string;
  parameter: boolean;
  dateCompletion: string;
}

export interface QuestionFeedback {
  id: number;
  libelle: string;
  type: string;
  obligatoire: boolean;
  choix?: string[] | null;
}

export interface FeedbackFormResponse {
  challenge: CodingChallengeDto;
  questions: QuestionFeedback[];
  alreadySubmitted: boolean;
}

export interface SubmitFeedbackRequest {
  codingChallengeId: number;
  noteGlobale: number | null;
  commentaire: string | null;
  statut: string;
  reponses: { questionId: number; valeur: string }[];
}

export interface FeedbackResponse {
  id: number;
  noteGlobale: number;
  commentaire: string;
  statut: string;
  createdAt: string;
  utilisateurId: number;
  utilisateurNom?: string | null;
  utilisateurPrenom?: string | null;
  utilisateurUserName?: string | null;
  utilisateurEmail?: string | null;
  codingChallengeId: number;
  challengeTitre?: string | null;
  challengeTag?: string | null;
  challengeDescription?: string | null;
  challengeSupprime?: boolean;
}

export interface DemandeReinitialisationDto {
  id: number;
  email: string;
  utilisateurId: number | null;
  utilisateurNom: string | null;
  utilisateurPrenom: string | null;
  statut: string;
  dateDemande: string;
  dateTraitement: string | null;
  traiteParId: number | null;
  traiteParEmail: string | null;
}

export interface IntegrationLogDto {
  id: number;
  type: string;
  statut: string;
  message: string;
  date: string;
  codingChallengeId: number | null;
}

export interface QuestionFeedbackDto {
  id: number;
  libelle: string;
  type: string;
  obligatoire: boolean;
  choix?: string[] | null;
}

export interface MeResponse {
  subject: string;
  issuer: string | null;
  roles: string[];
  uid: number;
  expiresAt: string | null;
}

export interface AverageScoreByTag {
  tag: string;
  averageScore: number;
}

export interface FeedbackParticipation {
  participationRate: number;
  totalChallenges: number;
  submittedFeedbacks: number;
}

export interface ChallengeRanking {
  challengeId: number;
  titre: string;
  metricValue: number;
  rankingType: string;
}

export interface ChallengeStatistics {
  challengeId: number;
  titre: string;
  averageScore: number;
  feedbackCount: number;
}

export interface UserDashboardKpi {
  notificationsTotal: number;
  notificationsPending: number;
  feedbacksSubmitted: number;
}

export interface ChallengeAdminDashboardKpi {
  challengesActifs: number;
  challengesArchives: number;
  notificationsTotal: number;
  notificationsEnAttente: number;
  feedbacksRecus: number;
  noteMoyenne: number;
}

export interface ManagerDashboardKpi {
  tauxParticipation: number;
  challengesTotal: number;
  feedbacksSoumis: number;
  noteMoyenneGlobale: number;
  notificationsEnAttente: number;
  tagsCouverts: number;
}

export interface AppAdminDashboardKpi {
  utilisateursStaff: number;
  utilisateursCandidats: number;
  questionsActives: number;
  demandesReinitEnAttente: number;
  logsErreur: number;
  challengesActifs: number;
  feedbacksSoumis: number;
  kafkaEnabled: boolean;
  notificationEnabled: boolean;
}
