import { useCallback, useEffect, useState } from 'react';
import Card from '../../components/Card';
import ErrorBanner from '../../components/ErrorBanner';
import PageHeader from '../../components/PageHeader';
import Pagination from '../../components/Pagination';
import Table from '../../components/Table';
import { deleteChallenge, getChallengesPage, syncChallenges } from '../../api/resources';
import type { CodingChallengeDto } from '../../api/types';
import { useI18n } from '../../i18n/I18nContext';

export default function AdminChallenges() {
  const { t } = useI18n();
  const [items, setItems] = useState<CodingChallengeDto[]>([]);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [archivingId, setArchivingId] = useState<number | null>(null);
  const [syncing, setSyncing] = useState(false);

  const reload = useCallback(
    (opts?: { quiet?: boolean }) => {
      if (!opts?.quiet) setLoading(true);
      setError('');
      return getChallengesPage(page, size)
        .then((data) => {
          setItems(data.content);
          setTotalPages(data.totalPages);
          setTotalElements(data.totalElements);
          if (data.content.length === 0 && data.totalPages > 0 && page > 0) {
            setPage((p) => Math.max(0, p - 1));
          }
        })
        .catch((err) => setError(err.response?.data?.message ?? 'Chargement impossible.'))
        .finally(() => setLoading(false));
    },
    [page, size]
  );

  useEffect(() => {
    void reload();
  }, [reload]);

  async function onSync() {
    setMessage('');
    setError('');
    setSyncing(true);
    try {
      const res = await syncChallenges();
      setMessage(`Synchronisation : ${res.published ?? 0} publié(s).`);
      await reload({ quiet: true });
    } catch (err: unknown) {
      setError(
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
          'Sync impossible.'
      );
    } finally {
      setSyncing(false);
    }
  }

  async function onArchive(c: CodingChallengeDto) {
    if (archivingId != null) return;
    if (!confirm(t('admin.archiveConfirm', { title: c.titre }))) return;
    setArchivingId(c.id);
    setError('');
    setMessage('');
    try {
      await deleteChallenge(c.id);
      setItems((prev) => prev.filter((x) => x.id !== c.id));
      setTotalElements((n) => Math.max(0, n - 1));
      setMessage(`« ${c.titre} » archivé.`);
      await reload({ quiet: true });
    } catch (err: unknown) {
      setError(
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
          'Archivage impossible.'
      );
    } finally {
      setArchivingId(null);
    }
  }

  return (
    <div>
      <PageHeader
        title={t('admin.challengesTitle')}
        subtitle={t('admin.challengesSubtitle')}
        action={
          <button
            type="button"
            onClick={onSync}
            disabled={syncing}
            className="rounded-lg bg-brand px-4 py-2 text-sm font-semibold text-white hover:bg-brand-dark disabled:opacity-60"
          >
            {syncing ? '…' : t('admin.sync')}
          </button>
        }
      />
      {error && <ErrorBanner message={error} />}
      {message && (
        <div className="mb-4 border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800 dark:border-emerald-900 dark:bg-emerald-950/40 dark:text-emerald-300">
          {message}
        </div>
      )}
      <Card>
        <Table
          columns={[
            t('inbox.challenge'),
            t('admin.tag'),
            t('admin.duration'),
            t('common.date'),
            t('common.action'),
          ]}
          isEmpty={!loading && items.length === 0}
          emptyLabel={loading ? t('common.loading') : t('common.empty')}
        >
          {items.map((c) => (
            <tr key={c.id} className="hover:bg-slate-50/60 dark:hover:bg-slate-800/60">
              <td className="px-5 py-3.5 font-medium text-slate-800 dark:text-slate-100">{c.titre}</td>
              <td className="px-5 py-3.5 text-slate-600 dark:text-slate-300">{c.tag}</td>
              <td className="px-5 py-3.5 text-slate-600 dark:text-slate-300">{c.duree}</td>
              <td className="px-5 py-3.5 text-slate-500 dark:text-slate-400">
                {c.dateCompletion ? new Date(c.dateCompletion).toLocaleDateString('fr-FR') : '—'}
              </td>
              <td className="px-5 py-3.5">
                <button
                  type="button"
                  disabled={archivingId === c.id}
                  onClick={() => void onArchive(c)}
                  className="text-xs font-semibold text-red-600 hover:text-red-700 disabled:opacity-50 dark:text-red-400"
                >
                  {archivingId === c.id ? '…' : t('common.archive')}
                </button>
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
