import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import Card from '../components/Card';
import ErrorBanner from '../components/ErrorBanner';
import FilterSelect from '../components/FilterSelect';
import PageHeader from '../components/PageHeader';
import Pagination from '../components/Pagination';
import SearchInput from '../components/SearchInput';
import Table from '../components/Table';
import { getUserId } from '../auth';
import {
  getChallengeTags,
  getFeedbackForm,
  getNotificationsByUserPage,
  submitFeedback,
} from '../api/resources';
import type { FeedbackFormResponse, NotificationDto, QuestionFeedback } from '../api/types';
import { useI18n } from '../i18n/I18nContext';

const NOTIF_STATUSES = ['EN_ATTENTE', 'ENVOYEE', 'LUE', 'ECHEC'];

/** While typing, allow incomplete decimals like "5." or ".5". */
function isNoteDraft(value: string): boolean {
  return /^\d*\.?\d*$/.test(value.trim());
}

function normalizeNote(value: string): string {
  let v = value.trim();
  if (v.endsWith('.') && v.length > 1) v = v.slice(0, -1);
  if (v.startsWith('.') && v.length > 1) v = `0${v}`;
  return v;
}

function isValidNote(value: string): boolean {
  const normalized = normalizeNote(value);
  if (!normalized) return false;
  const n = Number(normalized);
  return Number.isFinite(n) && n >= 0 && n <= 5;
}

export default function FeedbackFormPage() {
  const { t } = useI18n();
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const challengeIdParam = params.get('challengeId');
  const challengeId = challengeIdParam ? Number(challengeIdParam) : null;

  const [formData, setFormData] = useState<FeedbackFormResponse | null>(null);
  const [answers, setAnswers] = useState<string[]>([]);
  const [fieldErrors, setFieldErrors] = useState<string[]>([]);
  const [note, setNote] = useState(3);
  const [commentaire, setCommentaire] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const [picker, setPicker] = useState<NotificationDto[]>([]);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [search, setSearch] = useState('');
  const [statut, setStatut] = useState('');
  const [tag, setTag] = useState('');
  const [tags, setTags] = useState<string[]>([]);

  useEffect(() => {
    if (challengeId) return;
    getChallengeTags()
      .then(setTags)
      .catch(() => setTags([]));
  }, [challengeId]);

  useEffect(() => {
    if (challengeId) {
      setLoading(true);
      getFeedbackForm(challengeId)
        .then((data) => {
          setFormData(data);
          setAnswers(data.questions.map(() => ''));
          setFieldErrors(data.questions.map(() => ''));
        })
        .catch((err) => setError(err.response?.data?.message ?? 'Formulaire indisponible.'))
        .finally(() => setLoading(false));
      return;
    }

    const uid = getUserId();
    if (!uid) {
      setError('Session invalide.');
      setLoading(false);
      return;
    }
    setLoading(true);
    getNotificationsByUserPage(
      uid,
      page,
      size,
      search || undefined,
      statut || undefined,
      tag || undefined
    )
      .then((data) => {
        setPicker(data.content);
        setTotalPages(data.totalPages);
        setTotalElements(data.totalElements);
      })
      .catch((err) => setError(err.response?.data?.message ?? 'Chargement impossible.'))
      .finally(() => setLoading(false));
  }, [challengeId, page, size, search, statut, tag]);

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

  function setAnswer(index: number, value: string, question: QuestionFeedback) {
    setAnswers((prev) => {
      const next = [...prev];
      next[index] = value;
      return next;
    });
    setFieldErrors((prev) => {
      const next = [...prev];
      if (question.type === 'NOTE' && value.trim()) {
        const draft = value.trim();
        // Allow incomplete decimals while typing ("5.") — validate fully on blur/submit.
        const incomplete = draft.endsWith('.') && isNoteDraft(draft);
        next[index] = incomplete || isValidNote(draft) ? '' : t('feedbackForm.noteInvalid');
      } else {
        next[index] = '';
      }
      return next;
    });
  }

  function validateBeforeSubmit(): boolean {
    if (!formData) return false;
    const nextErrors = formData.questions.map((q, i) => {
      const value = (answers[i] ?? '').trim();
      if (q.obligatoire && !value) {
        return t('feedbackForm.fieldRequired');
      }
      if (q.type === 'NOTE' && value && !isValidNote(value)) {
        return t('feedbackForm.noteInvalid');
      }
      if (q.type === 'CHOIX' && value && !(q.choix ?? []).includes(value)) {
        return t('feedbackForm.choiceMissing');
      }
      return '';
    });
    setFieldErrors(nextErrors);
    return nextErrors.every((e) => !e);
  }

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!formData) return;
    if (!validateBeforeSubmit()) {
      setError(t('feedbackForm.fixAnswers'));
      return;
    }
    setSaving(true);
    setError('');
    try {
      await submitFeedback({
        codingChallengeId: formData.challenge.id,
        noteGlobale: note,
        commentaire,
        statut: 'SOUMIS',
        reponses: formData.questions.map((q, i) => ({
          questionId: q.id,
          valeur: q.type === 'NOTE' ? normalizeNote(answers[i] ?? '') : (answers[i] ?? ''),
        })),
      });
      navigate('/inbox');
    } catch (err: unknown) {
      const status = (err as { response?: { status?: number; data?: { message?: string } } })?.response
        ?.status;
      const message =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
        'Échec de soumission.';
      setError(
        status === 409
          ? t('feedbackForm.already')
          : message
      );
      setSaving(false);
    }
  }

  function renderQuestionInput(q: QuestionFeedback, i: number) {
    const value = answers[i] ?? '';
    const err = fieldErrors[i];
    const baseClass =
      'w-full rounded-lg border px-3 py-2.5 dark:bg-slate-800 dark:text-slate-100 ' +
      (err
        ? 'border-red-400 focus:border-red-500 focus:outline-none'
        : 'border-slate-300 dark:border-slate-600');

    if (q.type === 'NOTE') {
      return (
        <>
          <input
            type="text"
            inputMode="decimal"
            value={value}
            onChange={(e) => setAnswer(i, e.target.value, q)}
            onBlur={() => {
              const normalized = normalizeNote(value);
              if (normalized !== value) setAnswer(i, normalized, q);
            }}
            required={q.obligatoire}
            placeholder={t('feedbackForm.noteHint')}
            className={baseClass}
          />
          <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">{t('feedbackForm.noteHint')}</p>
        </>
      );
    }

    if (q.type === 'CHOIX') {
      const options = q.choix ?? [];
      if (options.length === 0) {
        return <p className="text-sm text-amber-700 dark:text-amber-300">{t('feedbackForm.choiceMissing')}</p>;
      }
      return (
        <select
          value={value}
          onChange={(e) => setAnswer(i, e.target.value, q)}
          required={q.obligatoire}
          className={baseClass}
        >
          <option value="">{t('feedbackForm.choicePlaceholder')}</option>
          {options.map((opt) => (
            <option key={opt} value={opt}>
              {opt}
            </option>
          ))}
        </select>
      );
    }

    return (
      <input
        type="text"
        value={value}
        onChange={(e) => setAnswer(i, e.target.value, q)}
        required={q.obligatoire}
        className={baseClass}
      />
    );
  }

  if (!challengeId) {
    return (
      <div>
        <PageHeader title={t('feedbackForm.title')} subtitle={t('feedbackForm.subtitle')} />
        {error && <ErrorBanner message={error} />}
        <Card>
          <div className="flex flex-wrap items-center gap-3 border-b border-slate-200 px-4 py-2.5 dark:border-slate-700">
            <SearchInput value={search} onChange={onSearchChange} className="max-w-xs" />
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
            columns={[t('inbox.challenge'), t('inbox.tag'), t('common.action')]}
            isEmpty={loading || picker.length === 0}
            emptyLabel={
              loading
                ? t('common.loading')
                : search || statut || tag
                  ? t('common.noResults')
                  : t('inbox.empty')
            }
          >
            {picker.map((n) => (
              <tr key={n.id} className="hover:bg-slate-50/60 dark:hover:bg-slate-800/60">
                <td className="px-4 py-3 font-medium">
                  {n.challengeTitre ?? `#${n.codingChallengeId}`}
                </td>
                <td className="px-4 py-3 text-slate-500 dark:text-slate-400">{n.challengeTag ?? '—'}</td>
                <td className="px-4 py-3">
                  <button
                    type="button"
                    onClick={() => navigate(`/feedback/form?challengeId=${n.codingChallengeId}`)}
                    className="rounded-lg bg-brand px-3.5 py-1.5 text-xs font-semibold text-white hover:bg-brand-dark"
                  >
                    {t('feedbackForm.fill')}
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

  if (loading) return <p className="text-slate-500 dark:text-slate-400">{t('common.loading')}</p>;
  if (!formData) return <p className="text-red-600 dark:text-red-400">{error || 'Formulaire introuvable.'}</p>;

  return (
    <div>
      <PageHeader
        title={formData.challenge.titre}
        subtitle={formData.challenge.description}
        backTo="/inbox"
      />
      {formData.alreadySubmitted && (
        <div className="mb-4 border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
          {t('feedbackForm.already')}
        </div>
      )}
      {error && <ErrorBanner message={error} />}
      <Card padded>
        <form onSubmit={onSubmit} className="space-y-5" noValidate>
          <label className="block">
            <span className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-200">
              {t('feedbackForm.globalNote')}
            </span>
            <input
              type="number"
              min={0}
              max={5}
              step="any"
              value={note}
              onChange={(e) => setNote(Number(e.target.value))}
              className="w-full rounded-lg border border-slate-300 dark:border-slate-600 px-3 py-2.5 dark:bg-slate-800 dark:text-slate-100"
              required
            />
          </label>
          <label className="block">
            <span className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-200">
              {t('feedbackForm.comment')}
            </span>
            <textarea
              value={commentaire}
              onChange={(e) => setCommentaire(e.target.value)}
              className="w-full rounded-lg border border-slate-300 dark:border-slate-600 px-3 py-2.5 dark:bg-slate-800 dark:text-slate-100"
              rows={3}
            />
          </label>
          {formData.questions.map((q, i) => (
            <label key={q.id} className="block">
              <span className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-200">
                {q.libelle}
                {q.obligatoire ? ' *' : ''}
                <span className="ml-2 text-xs font-normal text-slate-400">{q.type}</span>
              </span>
              {renderQuestionInput(q, i)}
              {fieldErrors[i] && (
                <p className="mt-1 text-xs font-medium text-red-600 dark:text-red-400">{fieldErrors[i]}</p>
              )}
            </label>
          ))}
          <button
            type="submit"
            disabled={saving || formData.alreadySubmitted}
            className="rounded-lg bg-brand px-5 py-2.5 font-semibold text-white hover:bg-brand-dark disabled:opacity-60"
          >
            {saving ? t('feedbackForm.submitting') : t('feedbackForm.submit')}
          </button>
        </form>
      </Card>
    </div>
  );
}
