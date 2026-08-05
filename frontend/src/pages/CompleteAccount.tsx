import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Lock, User } from 'lucide-react';
import { completeAccount, getSetupInfo } from '../api/authApi';
import BrandPanel from '../components/BrandPanel';
import ErrorBanner from '../components/ErrorBanner';
import FormField from '../components/FormField';
import LocaleThemeControls from '../components/LocaleThemeControls';
import type { SetupAccountInfo } from '../api/types';
import { useI18n } from '../i18n/I18nContext';

export default function CompleteAccount() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const { t } = useI18n();
  const token = params.get('token') ?? '';
  const challengeId = params.get('challengeId');

  const [info, setInfo] = useState<SetupAccountInfo | null>(null);
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [nom, setNom] = useState('');
  const [prenom, setPrenom] = useState('');
  const [userName, setUserName] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!token) return;
    getSetupInfo(token)
      .then((data) => {
        setInfo(data);
        setNom(data.nom ?? '');
        setPrenom(data.prenom ?? '');
        setUserName(data.userName ?? '');
      })
      .catch(() => setError(t('common.errorGeneric')));
  }, [token, t]);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    if (password.length < 8 || password !== confirm) {
      setError(t('common.errorGeneric'));
      return;
    }
    setLoading(true);
    try {
      await completeAccount({
        token,
        password,
        nom: nom || null,
        prenom: prenom || null,
        userName: userName || null,
      });
      if (challengeId) {
        navigate(`/feedback/form?challengeId=${challengeId}`);
      } else {
        navigate('/inbox');
      }
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
        <div className="w-full max-w-sm">
          <img
            src="/logo.png"
            alt={t('appName')}
            className="mb-8 h-9 w-auto dark:brightness-0 dark:invert lg:hidden"
          />
          {!token ? (
            <div className="border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-900 dark:bg-red-950/40 dark:text-red-300">
              {t('auth.invalidLink')}
            </div>
          ) : (
            <>
              <h2 className="text-3xl font-bold tracking-tight text-slate-800 dark:text-slate-100">
                {t('auth.activateTitle')}
              </h2>
              <p className="mt-2 mb-8 text-sm text-slate-500 dark:text-slate-400">
                {info?.email
                  ? `${t('auth.account')} : ${info.email}`
                  : t('auth.activateSubtitle')}
              </p>
              <form onSubmit={handleSubmit}>
                {(!info?.nom || !info?.prenom) && (
                  <>
                    <FormField label={t('auth.nom')} icon={User} value={nom} onChange={setNom} />
                    <FormField
                      label={t('auth.prenom')}
                      icon={User}
                      value={prenom}
                      onChange={setPrenom}
                    />
                  </>
                )}
                {!info?.userName && (
                  <FormField
                    label={`${t('auth.username')} (${t('common.optional')})`}
                    icon={User}
                    value={userName}
                    onChange={setUserName}
                  />
                )}
                <FormField
                  label={t('auth.password')}
                  icon={Lock}
                  type="password"
                  name="new-password"
                  autoComplete="new-password"
                  value={password}
                  onChange={setPassword}
                  minLength={8}
                />
                <FormField
                  label={t('auth.confirm')}
                  icon={Lock}
                  type="password"
                  name="confirm-password"
                  autoComplete="new-password"
                  value={confirm}
                  onChange={setConfirm}
                />
                {error && <ErrorBanner message={error} />}
                <button
                  type="submit"
                  disabled={loading || !info}
                  className="w-full bg-brand py-3 font-semibold text-white transition hover:bg-brand-dark disabled:opacity-60"
                >
                  {loading ? t('auth.activating') : t('auth.activate')}
                </button>
              </form>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
