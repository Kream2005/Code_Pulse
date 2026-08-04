import { useEffect, useState } from 'react';
import NavCard from '../../components/NavCard';
import PageHeader from '../../components/PageHeader';
import StatCard from '../../components/StatCard';
import {
  canManageChallenges,
  canManageQuestions,
  canManageUsers,
  canReadFeedbacks,
  canReadLogs,
  canSeeAnalytics,
  isAdminCodePulse,
  isAdminCodingChallenge,
  isManagerRh,
} from '../../auth';
import {
  getAppAdminDashboardKpis,
  getChallengeAdminDashboardKpis,
  getManagerDashboardKpis,
} from '../../api/resources';
import type {
  AppAdminDashboardKpi,
  ChallengeAdminDashboardKpi,
  ManagerDashboardKpi,
} from '../../api/types';
import { useI18n } from '../../i18n/I18nContext';

function fmtScore(n: number | undefined) {
  if (n == null || Number.isNaN(n)) return '—';
  return (Math.round(n * 10) / 10).toFixed(1);
}

function fmtPct(n: number | undefined) {
  if (n == null || Number.isNaN(n)) return '—';
  return `${Math.round(n * 10) / 10}%`;
}

export default function AdminDashboard() {
  const { t } = useI18n();
  const [challengeKpi, setChallengeKpi] = useState<ChallengeAdminDashboardKpi | null>(null);
  const [managerKpi, setManagerKpi] = useState<ManagerDashboardKpi | null>(null);
  const [appKpi, setAppKpi] = useState<AppAdminDashboardKpi | null>(null);

  useEffect(() => {
    if (isAdminCodePulse()) {
      getAppAdminDashboardKpis().then(setAppKpi).catch(() => setAppKpi(null));
      return;
    }
    if (isAdminCodingChallenge()) {
      getChallengeAdminDashboardKpis().then(setChallengeKpi).catch(() => setChallengeKpi(null));
    }
    if (isManagerRh()) {
      getManagerDashboardKpis().then(setManagerKpi).catch(() => setManagerKpi(null));
    }
  }, []);

  const showChallengeStrip = isAdminCodingChallenge() && !isAdminCodePulse();
  const showManagerStrip = isManagerRh() && !isAdminCodePulse();
  const showAppStrip = isAdminCodePulse();

  return (
    <div>
      <PageHeader title={t('admin.title')} subtitle={t('admin.subtitle')} />

      {showChallengeStrip && challengeKpi && (
        <div className="mb-5 grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6">
          <StatCard label={t('admin.kpiChallengesActive')} value={challengeKpi.challengesActifs} />
          <StatCard label={t('admin.kpiChallengesArchived')} value={challengeKpi.challengesArchives} />
          <StatCard label={t('admin.kpiNotifications')} value={challengeKpi.notificationsTotal} />
          <StatCard
            label={t('admin.kpiPending')}
            value={challengeKpi.notificationsEnAttente}
            accent="text-amber-600"
          />
          <StatCard
            label={t('admin.kpiFeedbacksReceived')}
            value={challengeKpi.feedbacksRecus}
            accent="text-emerald-600"
          />
          <StatCard label={t('admin.kpiAvgScore')} value={fmtScore(challengeKpi.noteMoyenne)} />
        </div>
      )}

      {showManagerStrip && managerKpi && (
        <div className="mb-5 grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6">
          <StatCard
            label={t('admin.participation')}
            value={fmtPct(managerKpi.tauxParticipation)}
            accent="text-brand"
          />
          <StatCard label={t('admin.challengesCount')} value={managerKpi.challengesTotal} />
          <StatCard
            label={t('admin.submitted')}
            value={managerKpi.feedbacksSoumis}
            accent="text-emerald-600"
          />
          <StatCard label={t('admin.kpiAvgScore')} value={fmtScore(managerKpi.noteMoyenneGlobale)} />
          <StatCard
            label={t('admin.kpiPending')}
            value={managerKpi.notificationsEnAttente}
            accent="text-amber-600"
          />
          <StatCard label={t('admin.kpiTags')} value={managerKpi.tagsCouverts} />
        </div>
      )}

      {showAppStrip && appKpi && (
        <div className="mb-5 grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5">
          <StatCard label={t('admin.kpiStaff')} value={appKpi.utilisateursStaff} />
          <StatCard label={t('admin.kpiCandidates')} value={appKpi.utilisateursCandidats} />
          <StatCard label={t('admin.kpiQuestions')} value={appKpi.questionsActives} />
          <StatCard
            label={t('admin.kpiReinit')}
            value={appKpi.demandesReinitEnAttente}
            accent="text-amber-600"
          />
          <StatCard
            label={t('admin.kpiErrors')}
            value={appKpi.logsErreur}
            accent="text-red-600"
          />
          <StatCard
            label={t('admin.kpiKafka')}
            value={appKpi.kafkaEnabled ? t('admin.on') : t('admin.off')}
            accent={appKpi.kafkaEnabled ? 'text-emerald-600' : 'text-slate-500'}
          />
          <StatCard
            label={t('admin.kpiMail')}
            value={appKpi.notificationEnabled ? t('admin.on') : t('admin.off')}
            accent={appKpi.notificationEnabled ? 'text-emerald-600' : 'text-slate-500'}
          />
          <StatCard label={t('admin.kpiChallengesActive')} value={appKpi.challengesActifs} />
          <StatCard label={t('admin.submitted')} value={appKpi.feedbacksSoumis} />
        </div>
      )}

      <div className="grid gap-3 sm:grid-cols-2">
        {canManageUsers() && (
          <NavCard to="/admin/users" title={t('admin.usersCard')} subtitle={t('admin.usersCardSub')} />
        )}
        {isAdminCodePulse() && (
          <NavCard
            to="/admin/password-requests"
            title={t('admin.resetCard')}
            subtitle={t('admin.resetCardSub')}
          />
        )}
        {canManageChallenges() && (
          <NavCard
            to="/admin/challenges"
            title={t('admin.challengesCard')}
            subtitle={t('admin.challengesCardSub')}
          />
        )}
        {canReadFeedbacks() && (
          <NavCard
            to="/admin/feedbacks"
            title={t('admin.feedbacksCard')}
            subtitle={t('admin.feedbacksCardSub')}
          />
        )}
        {canSeeAnalytics() && (
          <NavCard
            to="/admin/analytics"
            title={t('admin.analyticsCard')}
            subtitle={t('admin.analyticsCardSub')}
          />
        )}
        {canManageQuestions() && (
          <NavCard
            to="/admin/questions"
            title={t('admin.questionsTitle')}
            subtitle={t('admin.questionsSubtitle')}
          />
        )}
        {canReadLogs() && (
          <NavCard to="/admin/logs" title={t('admin.logsCard')} subtitle={t('admin.logsCardSub')} />
        )}
      </div>
    </div>
  );
}
