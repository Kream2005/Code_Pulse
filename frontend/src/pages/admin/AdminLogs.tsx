import { useEffect, useState } from 'react';
import Card from '../../components/Card';
import ErrorBanner from '../../components/ErrorBanner';
import PageHeader from '../../components/PageHeader';
import Pagination from '../../components/Pagination';
import StatusBadge from '../../components/StatusBadge';
import Table from '../../components/Table';
import { getLogsPage } from '../../api/resources';
import type { IntegrationLogDto } from '../../api/types';

export default function AdminLogs() {
  const [items, setItems] = useState<IntegrationLogDto[]>([]);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    setLoading(true);
    getLogsPage(page, size)
      .then((data) => {
        setItems(data.content);
        setTotalPages(data.totalPages);
        setTotalElements(data.totalElements);
      })
      .catch((err) => setError(err.response?.data?.message ?? 'Chargement impossible.'))
      .finally(() => setLoading(false));
  }, [page, size]);

  return (
    <div>
      <PageHeader title="Logs d'intégration" subtitle="Supervision technique." />
      {error && <ErrorBanner message={error} />}
      <Card>
        <Table
          columns={['Type', 'Statut', 'Message', 'Date']}
          isEmpty={loading || items.length === 0}
          emptyLabel={loading ? 'Chargement…' : 'Aucun log.'}
        >
          {items.map((l) => (
            <tr key={l.id} className="hover:bg-slate-50/60 dark:hover:bg-slate-800/60">
              <td className="px-5 py-3.5 font-medium">{l.type}</td>
              <td className="px-5 py-3.5">
                <StatusBadge status={l.statut} />
              </td>
              <td className="max-w-md truncate px-5 py-3.5 text-slate-600 dark:text-slate-300" title={l.message}>
                {l.message}
              </td>
              <td className="px-5 py-3.5 text-slate-500 dark:text-slate-400">
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
            setPage(0);
            setSize(s);
          }}
        />
      </Card>
    </div>
  );
}
