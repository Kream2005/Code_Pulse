import { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { clearToken } from '../auth';

/** Public entry from notification emails — always requires the intended recipient to sign in. */
export default function FeedbackFormEntry() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const challengeId = params.get('challengeId');

  useEffect(() => {
    clearToken();
    if (!challengeId) {
      navigate('/login', { replace: true });
      return;
    }
    const returnUrl = encodeURIComponent(`/feedback/form?challengeId=${challengeId}`);
    navigate(`/login?returnUrl=${returnUrl}`, { replace: true });
  }, [challengeId, navigate]);

  return null;
}
