import { useEffect, useState } from 'react';
import Card from '../../components/Card';
import ErrorBanner from '../../components/ErrorBanner';
import PageHeader from '../../components/PageHeader';
import Pagination from '../../components/Pagination';
import SearchInput from '../../components/SearchInput';
import FilterSelect from '../../components/FilterSelect';
import Table from '../../components/Table';
import { addQuestion, deleteQuestion, getQuestionsPage } from '../../api/resources';
import type { QuestionFeedbackDto } from '../../api/types';
import { useI18n } from '../../i18n/I18nContext';

const QUESTION_TYPES = ['TEXTE', 'NOTE', 'CHOIX'];

export default function AdminQuestions() {
  const { t } = useI18n();
  const [items, setItems] = useState<QuestionFeedbackDto[]>([]);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [libelle, setLibelle] = useState('');
  const [type, setType] = useState('TEXTE');
  const [obligatoire, setObligatoire] = useState(true);
  const [search, setSearch] = useState('');
  const [typeFilter, setTypeFilter] = useState('');

  function reload() {
    setLoading(true);
    getQuestionsPage(page, size, search, typeFilter)
      .then((data) => {
        setItems(data.content);
        setTotalPages(data.totalPages);
        setTotalElements(data.totalElements);
      })
      .catch((err) => setError(err.response?.data?.message ?? 'Chargement impossible.'))
      .finally(() => setLoading(false));
  }

  useEffect(reload, [page, size, search, typeFilter]);

  function onSearchChange(value: string) {
    setPage(0);
    setSearch(value);
  }

  function onTypeFilterChange(value: string) {
    setPage(0);
    setTypeFilter(value);
  }

  async function onAdd(e: React.FormEvent) {
    e.preventDefault();
    try {
      await addQuestion({ libelle, type, obligatoire });
      setLibelle('');
      reload();
    } catch (err: unknown) {
      setError(
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
          'Création impossible.'
      );
    }
  }

  return (
    <div>
      <PageHeader title="Questions" subtitle="Questions du formulaire de feedback." />
      {error && <ErrorBanner message={error} />}
      <Card padded className="mb-5">
        <form onSubmit={onAdd} className="flex flex-wrap gap-3">
          <input
            className="min-w-[220px] flex-1 rounded-lg border border-slate-300 dark:border-slate-600 px-3 py-2"
            placeholder="Libellé"
            value={libelle}
            onChange={(e) => setLibelle(e.target.value)}
            required
          />
          <select
            className="rounded-lg border border-slate-300 px-3 py-2 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
            value={type}
            onChange={(e) => setType(e.target.value)}
          >
            <option value="TEXTE">TEXTE</option>
            <option value="NOTE">NOTE</option>
            <option value="CHOIX">CHOIX</option>
          </select>
          <label className="flex items-center gap-2 text-sm text-slate-600 dark:text-slate-300">
            <input
              type="checkbox"
              checked={obligatoire}
              onChange={(e) => setObligatoire(e.target.checked)}
            />
            Obligatoire
          </label>
          <button
            type="submit"
            className="rounded-lg bg-brand px-4 py-2 font-semibold text-white hover:bg-brand-dark"
          >
            Ajouter
          </button>
        </form>
      </Card>
      <Card>
        <div className="flex flex-wrap items-center gap-3 border-b border-slate-200 px-4 py-2.5 dark:border-slate-700">
          <SearchInput value={search} onChange={onSearchChange} className="max-w-xs" />
          <FilterSelect
            value={typeFilter}
            onChange={onTypeFilterChange}
            allLabel={t('common.allTypes')}
            options={QUESTION_TYPES.map((ty) => ({ value: ty, label: ty }))}
          />
        </div>
        <Table
          columns={['Libellé', 'Type', 'Obligatoire', 'Action']}
          isEmpty={loading || items.length === 0}
          emptyLabel={
            loading ? 'Chargement…' : search || typeFilter ? t('common.noResults') : 'Aucune question.'
          }
        >
          {items.map((q) => (
            <tr key={q.id} className="hover:bg-slate-50/60 dark:hover:bg-slate-800/60">
              <td className="px-4 py-3 font-medium">{q.libelle}</td>
              <td className="px-4 py-3 text-slate-600 dark:text-slate-300">{q.type}</td>
              <td className="px-4 py-3 text-slate-600 dark:text-slate-300">{q.obligatoire ? 'Oui' : 'Non'}</td>
              <td className="px-4 py-3">
                <button
                  type="button"
                  onClick={async () => {
                    await deleteQuestion(q.id);
                    reload();
                  }}
                  className="text-xs font-semibold text-red-600 dark:text-red-400"
                >
                  Supprimer
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
