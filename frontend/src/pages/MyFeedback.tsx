import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import Card from '../components/Card';
import PageHeader from '../components/PageHeader';
import Pagination from '../components/Pagination';
import StatusBadge from '../components/StatusBadge';
import Table from '../components/Table';
import { getUserId } from '../auth';
import { getFeedbacksByUserPage } from '../api/resources';
import type { FeedbackResponse } from '../api/types';
import { useI18n } from '../i18n/I18nContext';

export default function MyFeedback() {
  const { t } = useI18n();
  const [items, setItems] = useState<FeedbackResponse[]>([]);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const uid = getUserId();
    if (!uid) {
      setError(t('common.errorGeneric'));
      setLoading(false);
      return;
    }
    setLoading(true);
    getFeedbacksByUserPage(uid, page, size)
      .then((data) => {
        setItems(data.content);
        setTotalPages(data.totalPages);
        setTotalElements(data.totalElements);
      })
      .catch((err) => setError(err.response?.data?.message ?? t('common.errorGeneric')))
      .finally(() => setLoading(false));
  }, [page, size, t]);

  return (
    <div>
      <PageHeader title={t('myFeedback.title')} subtitle={t('myFeedback.subtitle')} />
      {error && <p className="mb-4 text-red-600 dark:text-red-400">{error}</p>}
      <Card>
        <Table
          columns={[
            'ID',
            t('inbox.challenge'),
            t('myFeedback.note'),
            t('common.status'),
            t('common.date'),
            t('common.action'),
          ]}
          isEmpty={loading || items.length === 0}
          emptyLabel={loading ? t('common.loading') : t('myFeedback.empty')}
        >
          {items.map((f) => (
            <tr key={f.id} className="hover:bg-slate-50/60 dark:hover:bg-slate-800/60">
              <td className="px-5 py-3.5 text-slate-600 dark:text-slate-300">{f.id}</td>
              <td className="px-5 py-3.5 font-medium text-slate-800 dark:text-slate-100">
                {f.challengeTitre ?? `#${f.codingChallengeId}`}
                {f.challengeSupprime ? (
                  <span className="ml-2 text-xs text-amber-600">{t('common.archived')}</span>
                ) : null}
              </td>
              <td className="px-5 py-3.5 text-slate-600 dark:text-slate-300">
                {f.noteGlobale != null ? `${f.noteGlobale} / 5` : '—'}
              </td>
              <td className="px-5 py-3.5">
                <StatusBadge status={f.statut} />
              </td>
              <td className="px-5 py-3.5 text-slate-500 dark:text-slate-400">
                {f.createdAt ? new Date(f.createdAt).toLocaleDateString() : '—'}
              </td>
              <td className="px-5 py-3.5">
                <Link
                  to={`/feedback/form?challengeId=${f.codingChallengeId}`}
                  className="text-xs font-semibold text-brand hover:text-brand-dark"
                >
                  {t('common.view')}
                </Link>
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
