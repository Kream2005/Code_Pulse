import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { ArrowRight, Lock, Mail } from 'lucide-react';
import { login } from '../api/authApi';
import BrandPanel from '../components/BrandPanel';
import ErrorBanner from '../components/ErrorBanner';
import FormField from '../components/FormField';
import LocaleThemeControls from '../components/LocaleThemeControls';
import { isAdmin } from '../auth';
import { useI18n } from '../i18n/I18nContext';

export default function Login() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const { t } = useI18n();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await login(email, password);
      const returnUrl = params.get('returnUrl');
      if (returnUrl) {
        navigate(returnUrl);
        return;
      }
      navigate(isAdmin() ? '/admin' : '/inbox');
    } catch (err: unknown) {
      const message =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
        t('common.errorGeneric');
      setError(message);
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
            {t('auth.welcomeBack')}
          </h2>
          <p className="mt-2 mb-8 text-sm text-slate-500 dark:text-slate-400">{t('auth.loginSubtitle')}</p>

          <FormField
            label={t('auth.email')}
            icon={Mail}
            type="email"
            value={email}
            onChange={setEmail}
            placeholder="vous@exemple.com"
          />
          <FormField
            label={t('auth.password')}
            icon={Lock}
            type="password"
            value={password}
            onChange={setPassword}
            placeholder="••••••••"
          />

          {error && <ErrorBanner message={error} />}

          <button
            type="submit"
            disabled={loading}
            className="group flex w-full items-center justify-center gap-2 bg-brand py-3 font-semibold text-white transition hover:bg-brand-dark disabled:opacity-60"
          >
            {loading ? t('auth.signingIn') : t('auth.signIn')}
            {!loading && (
              <ArrowRight className="h-4 w-4 transition-transform group-hover:translate-x-0.5" />
            )}
          </button>

          <p className="mt-6 text-center text-sm">
            <Link to="/forgot-password" className="font-medium text-brand hover:text-brand-dark">
              {t('auth.forgot')}
            </Link>
          </p>
        </form>
      </div>
    </div>
  );
}
