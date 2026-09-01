import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import Card from '../../components/Card';
import ErrorBanner from '../../components/ErrorBanner';
import PageHeader from '../../components/PageHeader';
import Pagination from '../../components/Pagination';
import SearchInput from '../../components/SearchInput';
import FilterSelect from '../../components/FilterSelect';
import StatusBadge from '../../components/StatusBadge';
import Table from '../../components/Table';
import { getChallengeTags, getFeedbacksPage } from '../../api/resources';
import type { FeedbackResponse } from '../../api/types';
import { useI18n } from '../../i18n/I18nContext';

const FEEDBACK_STATUSES = ['EN_COURS', 'NON_SOUMIS', 'SOUMIS'];

function userLabel(f: FeedbackResponse) {
  const name = [f.utilisateurPrenom, f.utilisateurNom].filter(Boolean).join(' ').trim();
  const username = f.utilisateurUserName?.trim();
  if (name && username) return `${name} (@${username})`;
  if (name) return name;
  if (username) return `@${username}`;
  return f.utilisateurEmail ?? `#${f.utilisateurId}`;
}

export default function AdminFeedbacks() {
  const { t } = useI18n();
  const [items, setItems] = useState<FeedbackResponse[]>([]);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');
  const [statutFilter, setStatutFilter] = useState('');
  const [tagFilter, setTagFilter] = useState('');
  const [tags, setTags] = useState<string[]>([]);

  useEffect(() => {
    getChallengeTags()
      .then(setTags)
      .catch(() => setTags([]));
  }, []);

  useEffect(() => {
    setLoading(true);
    getFeedbacksPage(page, size, search || undefined, statutFilter || undefined, tagFilter || undefined)
      .then((data) => {
        setItems(data.content);
        setTotalPages(data.totalPages);
        setTotalElements(data.totalElements);
      })
      .catch((err) => setError(err.response?.data?.message ?? 'Chargement impossible.'))
      .finally(() => setLoading(false));
  }, [page, size, search, statutFilter, tagFilter]);

  function onSearchChange(value: string) {
    setPage(1);
    setSearch(value);
  }

  function onStatutFilterChange(value: string) {
    setPage(1);
    setStatutFilter(value);
  }

  function onTagFilterChange(value: string) {
    setPage(1);
    setTagFilter(value);
  }

  return (
    <div>
      <PageHeader title="Feedbacks" subtitle="Tous les retours soumis." />
      {error && <ErrorBanner message={error} />}
      <Card>
        <div className="flex flex-wrap items-center gap-3 border-b border-slate-200 px-4 py-2.5 dark:border-slate-700">
          <SearchInput value={search} onChange={onSearchChange} className="max-w-xs" />
          <FilterSelect
            value={statutFilter}
            onChange={onStatutFilterChange}
            allLabel={t('common.allStatuses')}
            options={FEEDBACK_STATUSES.map((s) => ({ value: s, label: s }))}
          />
          <FilterSelect
            value={tagFilter}
            onChange={onTagFilterChange}
            allLabel={t('common.allTags')}
            options={tags.map((tg) => ({ value: tg, label: tg }))}
          />
        </div>
        <Table
          columns={['ID', 'Utilisateur', 'Challenge', 'Note', 'Statut', 'Date', t('common.action')]}
          isEmpty={!loading && items.length === 0}
          emptyLabel={
            loading
              ? 'Chargement…'
              : search || statutFilter || tagFilter
                ? t('common.noResults')
                : 'Aucun feedback.'
          }
        >
          {items.map((f) => (
            <tr key={f.id} className="hover:bg-slate-50/60 dark:hover:bg-slate-800/60">
              <td className="px-4 py-3 text-slate-500">{f.id}</td>
              <td className="px-4 py-3">
                <div className="font-medium text-slate-800 dark:text-slate-100">{userLabel(f)}</div>
                {f.utilisateurEmail && (
                  <div className="text-xs text-slate-500 dark:text-slate-400">{f.utilisateurEmail}</div>
                )}
              </td>
              <td className="px-4 py-3">
                <span className="font-medium text-slate-800 dark:text-slate-100">
                  {f.challengeTitre ?? `#${f.codingChallengeId}`}
                </span>
                {f.challengeSupprime && (
                  <span className="ml-2 text-xs text-amber-600">{t('common.challengeArchived')}</span>
                )}
              </td>
              <td className="px-4 py-3">{f.noteGlobale ?? '—'}</td>
              <td className="px-4 py-3">
                <StatusBadge status={f.statut} />
              </td>
              <td className="px-4 py-3 text-slate-500 dark:text-slate-400">
                {f.createdAt ? new Date(f.createdAt).toLocaleDateString('fr-FR') : '—'}
              </td>
              <td className="px-4 py-3">
                {f.statut === 'SOUMIS' ? (
                  <Link
                    to={`/admin/feedbacks/${f.id}`}
                    className="text-xs font-semibold text-brand hover:text-brand-dark"
                  >
                    {t('common.view')}
                  </Link>
                ) : (
                  <span className="text-xs text-slate-400">—</span>
                )}
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
