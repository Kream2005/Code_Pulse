import { useEffect, useState } from 'react';
import Card from '../../components/Card';
import ErrorBanner from '../../components/ErrorBanner';
import PageHeader from '../../components/PageHeader';
import Pagination from '../../components/Pagination';
import SearchInput from '../../components/SearchInput';
import StatusBadge from '../../components/StatusBadge';
import Table from '../../components/Table';
import {
  getDemandesPage,
  rejectDemande,
  sendResetLink,
  setTemporaryPassword,
} from '../../api/resources';
import type { DemandeReinitialisationDto } from '../../api/types';
import { useI18n } from '../../i18n/I18nContext';

export default function PasswordRequests() {
  const { t } = useI18n();
  const [items, setItems] = useState<DemandeReinitialisationDto[]>([]);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [statut, setStatut] = useState('EN_ATTENTE');
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [temps, setTemps] = useState<Record<number, string>>({});
  const [workingId, setWorkingId] = useState<number | null>(null);

  function reload() {
    setLoading(true);
    getDemandesPage(page, size, statut || undefined, search || undefined)
      .then((data) => {
        setItems(data.content);
        setTotalPages(data.totalPages);
        setTotalElements(data.totalElements);
      })
      .catch((err) => setError(err.response?.data?.message ?? 'Chargement impossible.'))
      .finally(() => setLoading(false));
  }

  useEffect(reload, [page, size, statut, search]);

  function onSearchChange(value: string) {
    setPage(0);
    setSearch(value);
  }

  async function onSend(d: DemandeReinitialisationDto) {
    setWorkingId(d.id);
    try {
      await sendResetLink(d.id);
      setMessage(`Lien envoyé à ${d.email}`);
      reload();
    } catch (err: unknown) {
      setError(
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Échec.'
      );
    } finally {
      setWorkingId(null);
    }
  }

  async function onTemp(d: DemandeReinitialisationDto) {
    const password = (temps[d.id] || '').trim();
    if (password.length < 8) {
      setError('Mot de passe temporaire : 8 caractères minimum.');
      return;
    }
    setWorkingId(d.id);
    try {
      await setTemporaryPassword(d.id, password);
      setMessage(`Mot de passe temporaire défini pour ${d.email}`);
      setTemps((p) => {
        const n = { ...p };
        delete n[d.id];
        return n;
      });
      reload();
    } catch (err: unknown) {
      setError(
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Échec.'
      );
    } finally {
      setWorkingId(null);
    }
  }

  async function onReject(d: DemandeReinitialisationDto) {
    if (!confirm(`Rejeter la demande de ${d.email} ?`)) return;
    setWorkingId(d.id);
    try {
      await rejectDemande(d.id);
      reload();
    } finally {
      setWorkingId(null);
    }
  }

  return (
    <div>
      <PageHeader
        title="Demandes de réinitialisation"
        subtitle="Envoyer un lien, définir un mot de passe temporaire, ou rejeter."
        action={
          <select
            value={statut}
            onChange={(e) => {
              setPage(0);
              setStatut(e.target.value);
            }}
            className="rounded-lg border border-slate-300 px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
          >
            <option value="">Tous</option>
            <option value="EN_ATTENTE">EN_ATTENTE</option>
            <option value="LIEN_ENVOYE">LIEN_ENVOYE</option>
            <option value="MOT_DE_PASSE_TEMPORAIRE">MOT_DE_PASSE_TEMPORAIRE</option>
            <option value="REJETEE">REJETEE</option>
          </select>
        }
      />
      {error && <ErrorBanner message={error} />}
      {message && (
        <div className="mb-4 border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800">
          {message}
        </div>
      )}
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
          columns={['Email', 'Statut', 'Date', 'Actions']}
          isEmpty={loading || items.length === 0}
          emptyLabel={loading ? 'Chargement…' : search ? t('common.noResults') : 'Aucune demande.'}
        >
          {items.map((d) => (
            <tr key={d.id} className="hover:bg-slate-50/60 dark:hover:bg-slate-800/60">
              <td className="px-4 py-3 font-medium">{d.email}</td>
              <td className="px-4 py-3">
                <StatusBadge status={d.statut} />
              </td>
              <td className="px-4 py-3 text-slate-500 dark:text-slate-400">
                {new Date(d.dateDemande).toLocaleString('fr-FR')}
              </td>
              <td className="px-4 py-3">
                <div className="flex flex-col gap-2">
                  <div className="flex flex-wrap gap-2">
                    <button
                      type="button"
                      disabled={workingId === d.id || d.statut !== 'EN_ATTENTE'}
                      onClick={() => onSend(d)}
                      className="rounded-lg bg-brand px-3 py-1.5 text-xs font-semibold text-white disabled:opacity-40"
                    >
                      Envoyer lien
                    </button>
                    <button
                      type="button"
                      disabled={workingId === d.id || d.statut !== 'EN_ATTENTE'}
                      onClick={() => onReject(d)}
                      className="rounded-lg border border-slate-300 dark:border-slate-600 px-3 py-1.5 text-xs font-semibold disabled:opacity-40"
                    >
                      Rejeter
                    </button>
                  </div>
                  <div className="flex gap-2">
                    <input
                      type="password"
                      name={`temp-password-${d.id}`}
                      autoComplete="new-password"
                      placeholder="Mot de passe temp."
                      value={temps[d.id] ?? ''}
                      onChange={(e) => setTemps((p) => ({ ...p, [d.id]: e.target.value }))}
                      className="rounded-lg border border-slate-300 dark:border-slate-600 px-2 py-1 text-xs"
                      disabled={d.statut !== 'EN_ATTENTE'}
                    />
                    <button
                      type="button"
                      disabled={workingId === d.id || d.statut !== 'EN_ATTENTE'}
                      onClick={() => onTemp(d)}
                      className="rounded-lg border border-brand px-3 py-1.5 text-xs font-semibold text-brand disabled:opacity-40"
                    >
                      Définir
                    </button>
                  </div>
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
