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
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [libelle, setLibelle] = useState('');
  const [type, setType] = useState('TEXTE');
  const [obligatoire, setObligatoire] = useState(true);
  const [choix, setChoix] = useState<string[]>(['', '']);
  const [choiceDraft, setChoiceDraft] = useState('');
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
    setPage(1);
    setSearch(value);
  }

  function onTypeFilterChange(value: string) {
    setPage(1);
    setTypeFilter(value);
  }

  function onTypeChange(next: string) {
    setType(next);
    if (next === 'CHOIX' && choix.length < 2) {
      setChoix(['', '']);
    }
  }

  function updateChoice(index: number, value: string) {
    setChoix((prev) => prev.map((c, i) => (i === index ? value : c)));
  }

  function removeChoice(index: number) {
    setChoix((prev) => (prev.length <= 2 ? prev : prev.filter((_, i) => i !== index)));
  }

  function addChoice() {
    const trimmed = choiceDraft.trim();
    if (!trimmed) return;
    setChoix((prev) => [...prev, trimmed]);
    setChoiceDraft('');
  }

  async function onAdd(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    const cleanedChoix =
      type === 'CHOIX'
        ? Array.from(new Set(choix.map((c) => c.trim()).filter(Boolean)))
        : [];
    if (type === 'CHOIX' && cleanedChoix.length < 2) {
      setError(t('admin.choicesRequired'));
      return;
    }
    try {
      await addQuestion({
        libelle,
        type,
        obligatoire,
        ...(type === 'CHOIX' ? { choix: cleanedChoix } : { choix: [] }),
      });
      setLibelle('');
      setChoix(['', '']);
      setChoiceDraft('');
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
      <PageHeader title={t('admin.questionsTitle')} subtitle={t('admin.questionsSubtitle')} />
      {error && <ErrorBanner message={error} />}
      <Card padded className="mb-5">
        <form onSubmit={onAdd} className="space-y-3">
          <div className="flex flex-wrap gap-3">
            <input
              className="min-w-[220px] flex-1 rounded-lg border border-slate-300 dark:border-slate-600 px-3 py-2"
              placeholder={t('admin.label')}
              value={libelle}
              onChange={(e) => setLibelle(e.target.value)}
              required
            />
            <select
              className="rounded-lg border border-slate-300 px-3 py-2 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
              value={type}
              onChange={(e) => onTypeChange(e.target.value)}
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
              {t('admin.required')}
            </label>
            <button
              type="submit"
              className="rounded-lg bg-brand px-4 py-2 font-semibold text-white hover:bg-brand-dark"
            >
              {t('admin.add')}
            </button>
          </div>

          {type === 'CHOIX' && (
            <div className="rounded-lg border border-slate-200 bg-slate-50 p-3 dark:border-slate-700 dark:bg-slate-800/50">
              <p className="mb-2 text-sm font-medium text-slate-700 dark:text-slate-200">
                {t('admin.choices')}
              </p>
              <p className="mb-3 text-xs text-slate-500 dark:text-slate-400">{t('admin.choicesHint')}</p>
              <div className="space-y-2">
                {choix.map((option, index) => (
                  <div key={index} className="flex gap-2">
                    <input
                      className="min-w-0 flex-1 rounded-lg border border-slate-300 px-3 py-1.5 text-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
                      value={option}
                      onChange={(e) => updateChoice(index, e.target.value)}
                      placeholder={`${t('admin.choices')} ${index + 1}`}
                      required
                    />
                    <button
                      type="button"
                      onClick={() => removeChoice(index)}
                      disabled={choix.length <= 2}
                      className="rounded-lg border border-slate-300 px-2.5 text-xs font-semibold text-slate-600 disabled:opacity-40 dark:border-slate-600 dark:text-slate-300"
                    >
                      ×
                    </button>
                  </div>
                ))}
                <div className="flex gap-2">
                  <input
                    className="min-w-0 flex-1 rounded-lg border border-slate-300 px-3 py-1.5 text-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
                    value={choiceDraft}
                    onChange={(e) => setChoiceDraft(e.target.value)}
                    placeholder={t('admin.choicePlaceholder')}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter') {
                        e.preventDefault();
                        addChoice();
                      }
                    }}
                  />
                  <button
                    type="button"
                    onClick={addChoice}
                    className="rounded-lg border border-brand px-3 py-1.5 text-xs font-semibold text-brand hover:bg-brand/5"
                  >
                    {t('admin.addChoice')}
                  </button>
                </div>
              </div>
            </div>
          )}
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
          columns={[t('admin.label'), t('admin.type'), t('admin.choices'), t('admin.required'), t('common.action')]}
          isEmpty={loading || items.length === 0}
          emptyLabel={
            loading ? t('common.loading') : search || typeFilter ? t('common.noResults') : t('common.empty')
          }
        >
          {items.map((q) => (
            <tr key={q.id} className="hover:bg-slate-50/60 dark:hover:bg-slate-800/60">
              <td className="px-4 py-3 font-medium">{q.libelle}</td>
              <td className="px-4 py-3 text-slate-600 dark:text-slate-300">{q.type}</td>
              <td className="px-4 py-3 text-slate-600 dark:text-slate-300">
                {q.type === 'CHOIX' && q.choix?.length
                  ? q.choix.join(' · ')
                  : '—'}
              </td>
              <td className="px-4 py-3 text-slate-600 dark:text-slate-300">
                {q.obligatoire ? 'Oui' : 'Non'}
              </td>
              <td className="px-4 py-3">
                <button
                  type="button"
                  onClick={async () => {
                    await deleteQuestion(q.id);
                    reload();
                  }}
                  className="text-xs font-semibold text-red-600 dark:text-red-400"
                >
                  {t('common.delete')}
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
            setPage(1);
            setSize(s);
          }}
        />
      </Card>
    </div>
  );
}
