import { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { clearToken } from '../auth';

/** Public entry from notification emails — always requires the intended recipient to sign in. */
export default function FeedbackFormEntry() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const challengeId = params.get('challengeId');
  const recipientId = params.get('recipientId');

  useEffect(() => {
    clearToken();
    if (!challengeId) {
      navigate('/login', { replace: true });
      return;
    }
    const qs = new URLSearchParams({ challengeId });
    if (recipientId) qs.set('recipientId', recipientId);
    const returnUrl = encodeURIComponent(`/feedback/form?${qs.toString()}`);
    navigate(`/login?returnUrl=${returnUrl}`, { replace: true });
  }, [challengeId, recipientId, navigate]);

  return null;
}
