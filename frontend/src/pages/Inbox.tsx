import { useEffect, useState } from 'react';
import Card from '../components/Card';
import FilterSelect from '../components/FilterSelect';
import PageHeader from '../components/PageHeader';
import Pagination from '../components/Pagination';
import SearchInput from '../components/SearchInput';
import StatCard from '../components/StatCard';
import StatusBadge from '../components/StatusBadge';
import Table from '../components/Table';
import FeedbackRowAction from '../components/FeedbackRowAction';
import { getUserId } from '../auth';
import {
  getChallengeTags,
  getNotificationsByUserPage,
  getNotificationsPage,
  getUserDashboardKpis,
  updateNotificationStatut,
} from '../api/resources';
import type { NotificationDto, UserDashboardKpi } from '../api/types';
import { useI18n } from '../i18n/I18nContext';

const NOTIF_STATUSES = ['EN_ATTENTE', 'ENVOYEE', 'LUE', 'ECHEC'];

export default function Inbox({ admin = false }: { admin?: boolean }) {
  const { t } = useI18n();
  const [items, setItems] = useState<NotificationDto[]>([]);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [statut, setStatut] = useState('');
  const [tag, setTag] = useState('');
  const [tags, setTags] = useState<string[]>([]);
  const [search, setSearch] = useState('');
  const [kpi, setKpi] = useState<UserDashboardKpi | null>(null);

  useEffect(() => {
    if (!admin) {
      getUserDashboardKpis().then(setKpi).catch(() => setKpi(null));
    }
  }, [admin]);

  useEffect(() => {
    getChallengeTags()
      .then(setTags)
      .catch(() => setTags([]));
  }, []);

  useEffect(() => {
    setLoading(true);
    setError('');
    const load = admin
      ? getNotificationsPage(page, size, statut || undefined, search || undefined, tag || undefined)
      : (() => {
          const uid = getUserId();
          if (!uid) return Promise.reject(new Error(t('common.errorGeneric')));
          return getNotificationsByUserPage(
            uid,
            page,
            size,
            search || undefined,
            statut || undefined,
            tag || undefined
          );
        })();

    load
      .then((data) => {
        setItems(data.content);
        setTotalPages(data.totalPages);
        setTotalElements(data.totalElements);
      })
      .catch((err) => {
        setError(err.response?.data?.message ?? err.message ?? t('common.errorGeneric'));
      })
      .finally(() => setLoading(false));
  }, [admin, page, size, statut, tag, search, t]);

  function onSearchChange(value: string) {
    setPage(1);
    setSearch(value);
  }

  function onStatutChange(value: string) {
    setPage(1);
    setStatut(value);
  }

  function onTagChange(value: string) {
    setPage(1);
    setTag(value);
  }

  async function markRead(n: NotificationDto) {
    if (n.statut === 'LUE') return;
    await updateNotificationStatut(n.id, 'LUE');
    setItems((prev) => prev.map((x) => (x.id === n.id ? { ...x, statut: 'LUE' } : x)));
  }

  const hasFilters = Boolean(search || statut || tag);

  return (
    <div>
      <PageHeader
        title={admin ? t('inbox.adminTitle') : t('inbox.title')}
        subtitle={t('inbox.subtitle')}
      />
      {!admin && kpi && (
        <div className="mb-5 grid grid-cols-2 gap-3 sm:grid-cols-3">
          <StatCard label={t('admin.kpiNotifications')} value={kpi.notificationsTotal} />
          <StatCard
            label={t('admin.kpiPending')}
            value={kpi.notificationsPending}
            accent="text-amber-600"
          />
          <StatCard
            label={t('admin.kpiMyFeedbacks')}
            value={kpi.feedbacksSubmitted}
            accent="text-emerald-600"
          />
        </div>
      )}
      {error && <p className="mb-4 text-red-600 dark:text-red-400">{error}</p>}
      <Card>
        <div className="flex flex-wrap items-center gap-3 border-b border-slate-200 px-4 py-2.5 dark:border-slate-700">
          <SearchInput
            value={search}
            onChange={onSearchChange}
            placeholder={t('common.searchPlaceholder')}
            className="max-w-xs"
          />
          <FilterSelect
            value={statut}
            onChange={onStatutChange}
            allLabel={t('common.allStatuses')}
            options={NOTIF_STATUSES.map((s) => ({ value: s, label: s }))}
          />
          <FilterSelect
            value={tag}
            onChange={onTagChange}
            allLabel={t('common.allTags')}
            options={tags.map((tg) => ({ value: tg, label: tg }))}
          />
        </div>
        <Table
          columns={[
            t('inbox.challenge'),
            t('inbox.tag'),
            t('common.status'),
            t('common.date'),
            t('common.action'),
          ]}
          isEmpty={loading || items.length === 0}
          emptyLabel={loading ? t('common.loading') : hasFilters ? t('common.noResults') : t('inbox.empty')}
        >
          {items.map((n) => (
            <tr key={n.id} className="hover:bg-slate-50/60 dark:hover:bg-slate-800/60">
              <td className="px-4 py-3 font-medium text-slate-800 dark:text-slate-100">
                {n.challengeTitre ?? `Challenge #${n.codingChallengeId}`}
              </td>
              <td className="px-4 py-3 text-slate-600 dark:text-slate-300">{n.challengeTag ?? '—'}</td>
              <td className="px-4 py-3">
                <StatusBadge status={n.statut} />
              </td>
              <td className="px-4 py-3 text-slate-500 dark:text-slate-400">
                <div>{n.dateEnvoi ? new Date(n.dateEnvoi).toLocaleString() : '—'}</div>
                {(n.nombreRelances ?? 0) > 0 && (
                  <div className="mt-0.5 text-xs text-amber-600 dark:text-amber-400">
                    {t('inbox.reminded', { count: n.nombreRelances ?? 0 })}
                  </div>
                )}
              </td>
              <td className="px-4 py-3">
                <div className="flex flex-wrap gap-2">
                  <FeedbackRowAction
                    feedbackId={n.feedbackId}
                    feedbackStatut={n.feedbackStatut}
                    codingChallengeId={n.codingChallengeId}
                    variant="inbox"
                    admin={admin}
                  />
                  {!admin && n.statut !== 'LUE' && n.feedbackStatut !== 'SOUMIS' && (
                    <button
                      type="button"
                      onClick={() => markRead(n)}
                      className="inline-flex items-center rounded-lg border border-slate-300 px-3.5 py-1.5 text-xs font-semibold text-slate-600 transition hover:border-brand hover:text-brand dark:border-slate-600 dark:text-slate-200"
                    >
                      {t('common.markRead')}
                    </button>
                  )}
                </div>
              </td>
            </tr>
          ))}
        </Table>
        <Pagination
          page={page}
          totalPages={totalPages}
          totalElements={totalElements}
          size={size}
          onPageChange={setPage}
          onSizeChange={(s) => {
            setPage(1);
            setSize(s);
          }}
        />
      </Card>
    </div>
  );
}
