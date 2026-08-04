import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import Card from '../components/Card';
import ErrorBanner from '../components/ErrorBanner';
import PageHeader from '../components/PageHeader';
import Pagination from '../components/Pagination';
import SearchInput from '../components/SearchInput';
import Table from '../components/Table';
import { getUserId } from '../auth';
import {
  getFeedbackForm,
  getNotificationsByUserPage,
  submitFeedback,
} from '../api/resources';
import type { FeedbackFormResponse, NotificationDto } from '../api/types';

export default function FeedbackFormPage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const challengeIdParam = params.get('challengeId');
  const challengeId = challengeIdParam ? Number(challengeIdParam) : null;

  const [formData, setFormData] = useState<FeedbackFormResponse | null>(null);
  const [answers, setAnswers] = useState<string[]>([]);
  const [note, setNote] = useState(3);
  const [commentaire, setCommentaire] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const [picker, setPicker] = useState<NotificationDto[]>([]);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [search, setSearch] = useState('');

  useEffect(() => {
    if (challengeId) {
      setLoading(true);
      getFeedbackForm(challengeId)
        .then((data) => {
          setFormData(data);
          setAnswers(data.questions.map(() => ''));
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
    getNotificationsByUserPage(uid, page, size, search || undefined)
      .then((data) => {
        setPicker(data.content);
        setTotalPages(data.totalPages);
        setTotalElements(data.totalElements);
      })
      .catch((err) => setError(err.response?.data?.message ?? 'Chargement impossible.'))
      .finally(() => setLoading(false));
  }, [challengeId, page, size, search]);

  function onSearchChange(value: string) {
    setPage(0);
    setSearch(value);
  }

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!formData) return;
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
          valeur: answers[i] ?? '',
        })),
      });
      navigate('/inbox');
    } catch (err: unknown) {
      setError(
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
          'Échec de soumission.'
      );
      setSaving(false);
    }
  }

  if (!challengeId) {
    return (
      <div>
        <PageHeader
          title="Donner un feedback"
          subtitle="Choisissez un challenge depuis vos notifications."
        />
        {error && <ErrorBanner message={error} />}
        <Card>
          <div className="flex flex-wrap items-center gap-3 border-b border-slate-200 px-4 py-2.5 dark:border-slate-700">
            <SearchInput value={search} onChange={onSearchChange} className="max-w-xs" />
          </div>
          <Table
            columns={['Challenge', 'Tag', 'Action']}
            isEmpty={loading || picker.length === 0}
            emptyLabel={loading ? 'Chargement…' : search ? 'Aucun résultat.' : 'Aucune notification.'}
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
                    Remplir
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

  if (loading) return <p className="text-slate-500 dark:text-slate-400">Chargement…</p>;
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
          Feedback déjà soumis pour ce challenge.
        </div>
      )}
      {error && <ErrorBanner message={error} />}
      <Card padded>
        <form onSubmit={onSubmit} className="space-y-5">
          <label className="block">
            <span className="mb-1.5 block text-sm font-medium text-slate-700">Note globale (0–5)</span>
            <input
              type="number"
              min={0}
              max={5}
              value={note}
              onChange={(e) => setNote(Number(e.target.value))}
              className="w-full rounded-lg border border-slate-300 dark:border-slate-600 px-3 py-2.5"
              required
            />
          </label>
          <label className="block">
            <span className="mb-1.5 block text-sm font-medium text-slate-700">Commentaire</span>
            <textarea
              value={commentaire}
              onChange={(e) => setCommentaire(e.target.value)}
              className="w-full rounded-lg border border-slate-300 dark:border-slate-600 px-3 py-2.5"
              rows={3}
            />
          </label>
          {formData.questions.map((q, i) => (
            <label key={q.id} className="block">
              <span className="mb-1.5 block text-sm font-medium text-slate-700">
                {q.libelle}
                {q.obligatoire ? ' *' : ''}
              </span>
              <input
                value={answers[i] ?? ''}
                onChange={(e) =>
                  setAnswers((prev) => {
                    const next = [...prev];
                    next[i] = e.target.value;
                    return next;
                  })
                }
                required={q.obligatoire}
                className="w-full rounded-lg border border-slate-300 dark:border-slate-600 px-3 py-2.5"
              />
            </label>
          ))}
          <button
            type="submit"
            disabled={saving || formData.alreadySubmitted}
            className="rounded-lg bg-brand px-5 py-2.5 font-semibold text-white hover:bg-brand-dark disabled:opacity-60"
          >
            {saving ? 'Envoi…' : 'Soumettre'}
          </button>
        </form>
      </Card>
    </div>
  );
}
