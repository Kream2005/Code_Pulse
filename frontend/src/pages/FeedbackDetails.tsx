import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import Card from '../components/Card';
import ErrorBanner from '../components/ErrorBanner';
import PageHeader from '../components/PageHeader';
import StatusBadge from '../components/StatusBadge';
import { getFeedbackDetails } from '../api/resources';
import type { FeedbackDetailsResponse } from '../api/types';
import { useI18n } from '../i18n/I18nContext';

type Props = {
  backTo: string;
  backLabel?: string;
};

function userLabel(details: FeedbackDetailsResponse) {
  const f = details.feedback;
  const name = [f.utilisateurPrenom, f.utilisateurNom].filter(Boolean).join(' ').trim();
  const username = f.utilisateurUserName?.trim();
  if (name && username) return `${name} (@${username})`;
  if (name) return name;
  if (username) return `@${username}`;
  return f.utilisateurEmail ?? `#${f.utilisateurId}`;
}

export default function FeedbackDetails({ backTo, backLabel }: Props) {
  const { t } = useI18n();
  const { id } = useParams();
  const feedbackId = id ? Number(id) : NaN;

  const [data, setData] = useState<FeedbackDetailsResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!Number.isFinite(feedbackId)) {
      setError(t('feedbackDetails.invalid'));
      setLoading(false);
      return;
    }
    setLoading(true);
    setError('');
    getFeedbackDetails(feedbackId)
      .then(setData)
      .catch((err) =>
        setError(err.response?.data?.message ?? t('feedbackDetails.loadError'))
      )
      .finally(() => setLoading(false));
  }, [feedbackId, t]);

  const readOnlyClass =
    'w-full rounded-lg border border-slate-200 bg-slate-50 px-3 py-2.5 text-slate-800 dark:border-slate-700 dark:bg-slate-900/50 dark:text-slate-100';

  return (
    <div>
      <PageHeader
        title={t('feedbackDetails.title')}
        subtitle={t('feedbackDetails.subtitle')}
        action={
          <Link
            to={backTo}
            className="rounded-lg border border-slate-300 px-3 py-2 text-sm font-semibold text-slate-600 hover:border-brand hover:text-brand dark:border-slate-600 dark:text-slate-200"
          >
            {backLabel ?? t('common.back')}
          </Link>
        }
      />
      {error && <ErrorBanner message={error} />}
      {loading && <p className="text-sm text-slate-500">{t('common.loading')}</p>}
      {!loading && data && (
        <div className="space-y-4">
          <Card>
            <div className="space-y-4 p-5">
              <div className="flex flex-wrap items-center gap-3">
                <StatusBadge status={data.feedback.statut} />
                {data.feedback.createdAt && (
                  <span className="text-sm text-slate-500 dark:text-slate-400">
                    {new Date(data.feedback.createdAt).toLocaleString()}
                  </span>
                )}
              </div>

              <div>
                <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">
                  {t('inbox.challenge')}
                </p>
                <p className="mt-1 font-medium text-slate-800 dark:text-slate-100">
                  {data.feedback.challengeTitre ?? `#${data.feedback.codingChallengeId}`}
                  {data.feedback.challengeSupprime ? (
                    <span className="ml-2 text-xs font-normal text-amber-600">
                      {t('common.challengeArchived')}
                    </span>
                  ) : null}
                </p>
                {data.feedback.challengeTag && (
                  <p className="mt-0.5 text-sm text-slate-500 dark:text-slate-400">
                    {t('inbox.tag')}: {data.feedback.challengeTag}
                  </p>
                )}
              </div>

              <div>
                <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">
                  {t('feedbackDetails.author')}
                </p>
                <p className="mt-1 font-medium text-slate-800 dark:text-slate-100">
                  {userLabel(data)}
                </p>
                {data.feedback.utilisateurEmail && (
                  <p className="text-sm text-slate-500 dark:text-slate-400">
                    {data.feedback.utilisateurEmail}
                  </p>
                )}
              </div>

              <div>
                <label className="mb-1 block text-sm font-medium text-slate-700 dark:text-slate-200">
                  {t('feedbackForm.globalNote')}
                </label>
                <input
                  type="text"
                  readOnly
                  value={
                    data.feedback.noteGlobale != null ? `${data.feedback.noteGlobale} / 5` : '—'
                  }
                  className={readOnlyClass}
                />
              </div>

              <div>
                <label className="mb-1 block text-sm font-medium text-slate-700 dark:text-slate-200">
                  {t('feedbackForm.comment')}
                </label>
                <textarea
                  readOnly
                  rows={3}
                  value={data.feedback.commentaire?.trim() ? data.feedback.commentaire : '—'}
                  className={readOnlyClass}
                />
              </div>
            </div>
          </Card>

          <Card>
            <div className="border-b border-slate-200 px-5 py-3 dark:border-slate-700">
              <h2 className="text-sm font-semibold text-slate-800 dark:text-slate-100">
                {t('feedbackDetails.answers')}
              </h2>
            </div>
            <div className="space-y-5 p-5">
              {data.reponses.length === 0 ? (
                <p className="text-sm text-slate-500">{t('feedbackDetails.noAnswers')}</p>
              ) : (
                data.reponses.map((r, index) => (
                  <div key={r.id}>
                    <div className="mb-1.5 flex flex-wrap items-center gap-2">
                      <p className="text-sm font-medium text-slate-800 dark:text-slate-100">
                        {index + 1}. {r.questionLibelle ?? t('feedbackDetails.unknownQuestion')}
                      </p>
                      {r.questionType && (
                        <span className="rounded bg-slate-100 px-1.5 py-0.5 text-[10px] font-semibold uppercase tracking-wide text-slate-500 dark:bg-slate-800 dark:text-slate-400">
                          {r.questionType}
                        </span>
                      )}
                      {r.questionSupprime && (
                        <span className="text-xs font-medium text-amber-600">
                          {t('common.questionArchived')}
                        </span>
                      )}
                    </div>
                    {r.questionType === 'NOTE' ? (
                      <input
                        type="text"
                        readOnly
                        value={r.valeur?.trim() ? r.valeur : '—'}
                        className={readOnlyClass}
                      />
                    ) : (
                      <textarea
                        readOnly
                        rows={r.questionType === 'TEXTE' ? 3 : 2}
                        value={r.valeur?.trim() ? r.valeur : '—'}
                        className={readOnlyClass}
                      />
                    )}
                  </div>
                ))
              )}
            </div>
          </Card>
        </div>
      )}
    </div>
  );
}
