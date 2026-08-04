import { useEffect, useState } from 'react';
import Card from '../components/Card';
import PageHeader from '../components/PageHeader';
import client from '../api/client';
import { useI18n } from '../i18n/I18nContext';

type Profile = {
  email: string;
  nom: string | null;
  prenom: string | null;
  userName: string | null;
};

export default function Profile() {
  const { t } = useI18n();
  const [profile, setProfile] = useState<Profile | null>(null);
  const [error, setError] = useState('');

  useEffect(() => {
    client
      .get<Profile>('/api/profile')
      .then((r) => setProfile(r.data))
      .catch(() => setError(t('profile.loadError')));
  }, [t]);

  return (
    <div>
      <PageHeader title={t('profile.title')} subtitle={t('profile.subtitle')} />
      {error && <p className="mb-4 text-red-600 dark:text-red-400">{error}</p>}
      <Card padded className="max-w-xl">
        <dl className="space-y-3 text-sm">
          <div>
            <dt className="text-slate-500 dark:text-slate-400">{t('profile.email')}</dt>
            <dd className="font-medium text-slate-800 dark:text-slate-100">{profile?.email ?? '—'}</dd>
          </div>
          <div>
            <dt className="text-slate-500 dark:text-slate-400">{t('profile.nom')}</dt>
            <dd className="font-medium text-slate-800 dark:text-slate-100">{profile?.nom || '—'}</dd>
          </div>
          <div>
            <dt className="text-slate-500 dark:text-slate-400">{t('profile.prenom')}</dt>
            <dd className="font-medium text-slate-800 dark:text-slate-100">{profile?.prenom || '—'}</dd>
          </div>
          <div>
            <dt className="text-slate-500 dark:text-slate-400">{t('profile.username')}</dt>
            <dd className="font-medium text-slate-800 dark:text-slate-100">{profile?.userName || '—'}</dd>
          </div>
        </dl>
      </Card>
    </div>
  );
}
