import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Mail } from 'lucide-react';
import { forgotPassword } from '../api/authApi';
import BrandPanel from '../components/BrandPanel';
import ErrorBanner from '../components/ErrorBanner';
import FormField from '../components/FormField';
import LocaleThemeControls from '../components/LocaleThemeControls';
import { useI18n } from '../i18n/I18nContext';

export default function ForgotPassword() {
  const { t } = useI18n();
  const [email, setEmail] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res = await forgotPassword(email);
      setMessage(res.message);
    } catch {
      setError(t('common.errorGeneric'));
    } finally {
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
            {t('auth.forgotTitle')}
          </h2>
          <p className="mt-2 mb-8 text-sm text-slate-500 dark:text-slate-400">{t('auth.forgotSubtitle')}</p>
          <FormField
            label={t('auth.email')}
            icon={Mail}
            type="email"
            value={email}
            onChange={setEmail}
            placeholder="vous@exemple.com"
          />
          {error && <ErrorBanner message={error} />}
          {message && (
            <div className="mb-4 border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800 dark:border-emerald-900 dark:bg-emerald-950/40 dark:text-emerald-300">
              {message}
            </div>
          )}
          <button
            type="submit"
            disabled={loading}
            className="w-full bg-brand py-3 font-semibold text-white transition hover:bg-brand-dark disabled:opacity-60"
          >
            {loading ? t('auth.sending') : t('auth.sendRequest')}
          </button>
          <p className="mt-6 text-center text-sm">
            <Link to="/login" className="font-medium text-brand hover:text-brand-dark">
              {t('auth.backToLogin')}
            </Link>
          </p>
        </form>
      </div>
    </div>
  );
}
