import { Link } from 'react-router-dom';
import { useI18n } from '../i18n/I18nContext';

type Props = {
  feedbackId?: number | null;
  feedbackStatut?: string | null;
  codingChallengeId: number;
  /** Inbox: primary button for pending feedback; default text link like Mes feedbacks */
  variant?: 'inbox' | 'text';
  admin?: boolean;
};

export default function FeedbackRowAction({
  feedbackId,
  feedbackStatut,
  codingChallengeId,
  variant = 'text',
  admin = false,
}: Props) {
  const { t } = useI18n();
  const submitted = feedbackStatut === 'SOUMIS' && feedbackId != null;

  if (submitted) {
    const to = admin ? `/admin/feedbacks/${feedbackId}` : `/feedback/${feedbackId}`;
    return (
      <Link to={to} className="text-xs font-semibold text-brand hover:text-brand-dark">
        {t('common.view')}
      </Link>
    );
  }

  const formTo = `/feedback/form?challengeId=${codingChallengeId}`;

  if (variant === 'inbox') {
    return (
      <Link
        to={formTo}
        className="inline-flex items-center rounded-lg bg-brand px-3.5 py-1.5 text-xs font-semibold text-white transition hover:bg-brand-dark"
      >
        {t('common.feedback')}
      </Link>
    );
  }

  return (
    <Link to={formTo} className="text-xs font-semibold text-brand hover:text-brand-dark">
      {t('feedbackForm.fill')}
    </Link>
  );
}
