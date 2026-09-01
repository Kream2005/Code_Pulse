import { useEffect, useState } from 'react';
import Card from '../../components/Card';
import ErrorBanner from '../../components/ErrorBanner';
import PageHeader from '../../components/PageHeader';
import Pagination from '../../components/Pagination';
import SearchInput from '../../components/SearchInput';
import FilterSelect from '../../components/FilterSelect';
import StatusBadge from '../../components/StatusBadge';
import Table from '../../components/Table';
import { getLogsPage } from '../../api/resources';
import type { IntegrationLogDto } from '../../api/types';
import { useI18n } from '../../i18n/I18nContext';

const LOG_TYPES = [
  'SYNC_CHALLENGE',
  'ENVOI_NOTIFICATION',
  'RELANCE',
  'EXPORT_DONNEES',
  'AUTH',
  'FEEDBACK',
  'GESTION_UTILISATEUR',
  'AUTHORISATION',
  'GESTION_CHALLENGE',
  'DEMANDE_REINIT',
  'CONSULTATION',
  'CONFIG',
  'SYSTEME',
];
const LOG_STATUSES = ['INFO', 'SUCCES', 'WARNING', 'ERREUR', 'CRITIQUE'];

export default function AdminLogs() {
  const { t } = useI18n();
  const [items, setItems] = useState<IntegrationLogDto[]>([]);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');
  const [typeFilter, setTypeFilter] = useState('');
  const [statutFilter, setStatutFilter] = useState('');

  useEffect(() => {
    setLoading(true);
    getLogsPage(page, size, search, typeFilter, statutFilter)
      .then((data) => {
        setItems(data.content);
        setTotalPages(data.totalPages);
        setTotalElements(data.totalElements);
      })
      .catch((err) => setError(err.response?.data?.message ?? 'Chargement impossible.'))
      .finally(() => setLoading(false));
  }, [page, size, search, typeFilter, statutFilter]);

  function onSearchChange(value: string) {
    setPage(1);
    setSearch(value);
  }

  function onTypeFilterChange(value: string) {
    setPage(1);
    setTypeFilter(value);
  }

  function onStatutFilterChange(value: string) {
    setPage(1);
    setStatutFilter(value);
  }

  return (
    <div>
      <PageHeader title="Logs d'intégration" subtitle="Supervision technique." />
      {error && <ErrorBanner message={error} />}
      <Card>
        <div className="flex flex-wrap items-center gap-3 border-b border-slate-200 px-4 py-2.5 dark:border-slate-700">
          <SearchInput value={search} onChange={onSearchChange} className="max-w-xs" />
          <FilterSelect
            value={typeFilter}
            onChange={onTypeFilterChange}
            allLabel={t('common.allTypes')}
            options={LOG_TYPES.map((ty) => ({ value: ty, label: ty }))}
          />
          <FilterSelect
            value={statutFilter}
            onChange={onStatutFilterChange}
            allLabel={t('common.allStatuses')}
            options={LOG_STATUSES.map((s) => ({ value: s, label: s }))}
          />
        </div>
        <Table
          columns={['Type', 'Statut', 'Message', 'Date']}
          isEmpty={loading || items.length === 0}
          emptyLabel={
            loading ? 'Chargement…' : search || typeFilter || statutFilter ? t('common.noResults') : 'Aucun log.'
          }
        >
          {items.map((l) => (
            <tr key={l.id} className="hover:bg-slate-50/60 dark:hover:bg-slate-800/60">
              <td className="px-4 py-3 font-medium">{l.type}</td>
              <td className="px-4 py-3">
                <StatusBadge status={l.statut} />
              </td>
              <td className="max-w-md truncate px-4 py-3 text-slate-600 dark:text-slate-300" title={l.message}>
                {l.message}
              </td>
              <td className="px-4 py-3 text-slate-500 dark:text-slate-400">
                {l.date ? new Date(l.date).toLocaleString('fr-FR') : '—'}
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
