import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Lock, User } from 'lucide-react';
import { completeAccount, getSetupInfo } from '../api/authApi';
import { clearToken } from '../auth';
import BrandPanel from '../components/BrandPanel';
import ErrorBanner from '../components/ErrorBanner';
import FormField from '../components/FormField';
import LocaleThemeControls from '../components/LocaleThemeControls';
import type { SetupAccountInfo } from '../api/types';
import { useI18n } from '../i18n/I18nContext';

function hasText(value?: string | null) {
  return Boolean(value && value.trim());
}

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
  const [loadingInfo, setLoadingInfo] = useState(Boolean(token));

  useEffect(() => {
    if (token) clearToken();
  }, [token]);

  useEffect(() => {
    if (!token) return;
    setLoadingInfo(true);
    setError('');
    getSetupInfo(token)
      .then((data) => {
        setInfo(data);
        setNom(data.nom ?? '');
        setPrenom(data.prenom ?? '');
        setUserName(data.userName ?? '');
      })
      .catch((err: unknown) => {
        const message =
          (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
          t('auth.setupLoadError');
        setError(message);
        setInfo(null);
      })
      .finally(() => setLoadingInfo(false));
  }, [token, t]);

  const needNom = !hasText(info?.nom);
  const needPrenom = !hasText(info?.prenom);
  const needUserName = !hasText(info?.userName);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    if (needNom && !hasText(nom)) {
      setError(t('auth.profileRequired'));
      return;
    }
    if (needPrenom && !hasText(prenom)) {
      setError(t('auth.profileRequired'));
      return;
    }
    if (password.length < 8 || password !== confirm) {
      setError(t('auth.passwordMismatch'));
      return;
    }
    setLoading(true);
    try {
      await completeAccount({
        token,
        password,
        nom: needNom ? nom.trim() : null,
        prenom: needPrenom ? prenom.trim() : null,
        userName: needUserName && hasText(userName) ? userName.trim() : null,
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
              <p className="mt-2 mb-6 text-sm text-slate-500 dark:text-slate-400">
                {info?.email
                  ? `${t('auth.account')} : ${info.email}`
                  : t('auth.activateSubtitle')}
              </p>

              {loadingInfo && <p className="mb-4 text-sm text-slate-500">{t('common.loading')}</p>}

              {info && (
                <div className="mb-6 space-y-1 rounded-lg border border-slate-200 bg-slate-50 px-3 py-3 text-sm dark:border-slate-700 dark:bg-slate-900/40">
                  {hasText(info.prenom) || hasText(info.nom) ? (
                    <p className="text-slate-700 dark:text-slate-200">
                      <span className="text-slate-500">{t('auth.name')}: </span>
                      {[info.prenom, info.nom].filter(hasText).join(' ')}
                    </p>
                  ) : null}
                  {hasText(info.userName) ? (
                    <p className="text-slate-700 dark:text-slate-200">
                      <span className="text-slate-500">{t('auth.username')}: </span>
                      {info.userName}
                    </p>
                  ) : null}
                  {!needNom && !needPrenom && !needUserName ? (
                    <p className="pt-1 text-xs text-slate-500">{t('auth.onlyPasswordNeeded')}</p>
                  ) : null}
                </div>
              )}

              {info && (
                <form onSubmit={handleSubmit}>
                  {needNom && (
                    <FormField label={t('auth.nom')} icon={User} value={nom} onChange={setNom} />
                  )}
                  {needPrenom && (
                    <FormField
                      label={t('auth.prenom')}
                      icon={User}
                      value={prenom}
                      onChange={setPrenom}
                    />
                  )}
                  {needUserName && (
                    <FormField
                      label={`${t('auth.username')} (${t('common.optional')})`}
                      icon={User}
                      value={userName}
                      onChange={setUserName}
                      required={false}
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
                    disabled={loading || loadingInfo}
                    className="w-full bg-brand py-3 font-semibold text-white transition hover:bg-brand-dark disabled:opacity-60"
                  >
                    {loading ? t('auth.activating') : t('auth.activate')}
                  </button>
                </form>
              )}
              {!info && error && <ErrorBanner message={error} />}
            </>
          )}
        </div>
      </div>
    </div>
  );
}
