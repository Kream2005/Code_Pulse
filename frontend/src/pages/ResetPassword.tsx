import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Lock } from 'lucide-react';
import { getResetInfo, resetPassword } from '../api/authApi';
import BrandPanel from '../components/BrandPanel';
import ErrorBanner from '../components/ErrorBanner';
import FormField from '../components/FormField';
import LocaleThemeControls from '../components/LocaleThemeControls';
import { useI18n } from '../i18n/I18nContext';

export default function ResetPassword() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const { t } = useI18n();
  const token = params.get('token') ?? '';
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!token) return;
    getResetInfo(token)
      .then((d) => setEmail(d.email))
      .catch(() => setError(t('common.errorGeneric')));
  }, [token, t]);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (password !== confirm) {
      setError(t('common.errorGeneric'));
      return;
    }
    setLoading(true);
    setError('');
    try {
      await resetPassword(token, password);
      navigate('/inbox');
    } catch (err: unknown) {
      setError(
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
          t('common.errorGeneric')
      );
      setLoading(false);
    }
  }

  return (
    <div className="flex min-h-screen bg-slate-50 dark:bg-slate-950">
      <BrandPanel />
      <div className="relative flex w-full items-center justify-center p-8 lg:w-1/2">
        <LocaleThemeControls className="absolute right-6 top-6" />
        <form onSubmit={handleSubmit} className="w-full max-w-sm">
          <img
            src="/logo.png"
            alt={t('appName')}
            className="mb-8 h-9 w-auto dark:brightness-0 dark:invert lg:hidden"
          />
          <h2 className="text-3xl font-bold tracking-tight text-slate-800 dark:text-slate-100">
            {t('auth.resetTitle')}
          </h2>
          <p className="mt-2 mb-8 text-sm text-slate-500 dark:text-slate-400">
            {email ? `${t('auth.account')} : ${email}` : t('auth.resetSubtitle')}
          </p>
          <FormField
            label={t('auth.password')}
            icon={Lock}
            type="password"
            value={password}
            onChange={setPassword}
            minLength={8}
          />
          <FormField
            label={t('auth.confirm')}
            icon={Lock}
            type="password"
            value={confirm}
            onChange={setConfirm}
          />
          {error && <ErrorBanner message={error} />}
          <button
            type="submit"
            disabled={loading || !token}
            className="w-full bg-brand py-3 font-semibold text-white transition hover:bg-brand-dark disabled:opacity-60"
          >
            {loading ? t('auth.saving') : t('common.save')}
          </button>
        </form>
      </div>
    </div>
  );
}
