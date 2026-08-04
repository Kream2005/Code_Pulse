import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import Card from '../components/Card';
import PageHeader from '../components/PageHeader';
import Pagination from '../components/Pagination';
import SearchInput from '../components/SearchInput';
import StatCard from '../components/StatCard';
import StatusBadge from '../components/StatusBadge';
import Table from '../components/Table';
import { getUserId } from '../auth';
import {
  getNotificationsByUserPage,
  getNotificationsPage,
  getUserDashboardKpis,
  updateNotificationStatut,
} from '../api/resources';
import type { NotificationDto, UserDashboardKpi } from '../api/types';
import { useI18n } from '../i18n/I18nContext';

export default function Inbox({ admin = false }: { admin?: boolean }) {
  const { t } = useI18n();
  const [items, setItems] = useState<NotificationDto[]>([]);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [statut, setStatut] = useState('');
  const [search, setSearch] = useState('');
  const [kpi, setKpi] = useState<UserDashboardKpi | null>(null);

  useEffect(() => {
    if (!admin) {
      getUserDashboardKpis().then(setKpi).catch(() => setKpi(null));
    }
  }, [admin]);

  useEffect(() => {
    setLoading(true);
    setError('');
    const load = admin
      ? getNotificationsPage(page, size, statut || undefined, search || undefined)
      : (() => {
          const uid = getUserId();
          if (!uid) return Promise.reject(new Error(t('common.errorGeneric')));
          return getNotificationsByUserPage(uid, page, size, search || undefined, statut || undefined);
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
  }, [admin, page, size, statut, search, t]);

  function onSearchChange(value: string) {
    setPage(0);
    setSearch(value);
  }

  async function markRead(n: NotificationDto) {
    if (n.statut === 'LUE') return;
    await updateNotificationStatut(n.id, 'LUE');
    setItems((prev) => prev.map((x) => (x.id === n.id ? { ...x, statut: 'LUE' } : x)));
  }

  return (
    <div>
      <PageHeader
        title={admin ? t('inbox.adminTitle') : t('inbox.title')}
        subtitle={t('inbox.subtitle')}
        action={
          admin ? (
            <select
              value={statut}
              onChange={(e) => {
                setPage(0);
                setStatut(e.target.value);
              }}
              className="rounded-lg border border-slate-300 px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
            >
              <option value="">{t('common.allStatuses')}</option>
              <option value="EN_ATTENTE">EN_ATTENTE</option>
              <option value="ENVOYEE">ENVOYEE</option>
              <option value="LUE">LUE</option>
              <option value="ECHEC">ECHEC</option>
            </select>
          ) : undefined
        }
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
          emptyLabel={loading ? t('common.loading') : search ? t('common.noResults') : t('inbox.empty')}
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
                {n.dateEnvoi ? new Date(n.dateEnvoi).toLocaleString() : '—'}
              </td>
              <td className="px-4 py-3">
                <div className="flex flex-wrap gap-2">
                  <Link
                    to={`/feedback/form?challengeId=${n.codingChallengeId}`}
                    className="inline-flex items-center rounded-lg bg-brand px-3.5 py-1.5 text-xs font-semibold text-white transition hover:bg-brand-dark"
                  >
                    {t('common.feedback')}
                  </Link>
                  {!admin && n.statut !== 'LUE' && (
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
            setPage(0);
            setSize(s);
          }}
        />
      </Card>
    </div>
  );
}
