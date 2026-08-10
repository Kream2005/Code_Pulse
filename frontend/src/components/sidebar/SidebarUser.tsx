import { LogOut } from 'lucide-react';
import { useI18n } from '../../i18n/I18nContext';
import { labelClass } from './labelClass';

export default function SidebarUser({
  name,
  role,
  collapsed,
  onLogout,
}: {
  name: string | null;
  role: string | null;
  collapsed: boolean;
  onLogout: () => void;
}) {
  const { t } = useI18n();
  const initials = (name ?? '?')
    .split(/[@.\s]/)
    .map((p) => p[0])
    .slice(0, 2)
    .join('')
    .toUpperCase();

  return (
    <div className="border-t border-slate-200 p-3 dark:border-slate-700">
      <div className="mb-3 flex items-center gap-3">
        <div
          title={name ?? ''}
          className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-brand text-sm font-semibold text-white"
        >
          {initials}
        </div>
        <div className={labelClass(collapsed)}>
          <p className="truncate text-sm font-semibold text-slate-800 dark:text-slate-100">
            {name ?? '—'}
          </p>
          <p className="text-xs text-slate-500 dark:text-slate-400">{role}</p>
        </div>
      </div>
      <button
        onClick={onLogout}
        title={t('nav.logout')}
        className="flex w-full cursor-pointer items-center justify-center gap-2 rounded-lg border border-slate-300 px-3 py-2 text-sm font-medium text-slate-600 transition hover:bg-red-100 dark:border-slate-600 dark:text-slate-200 dark:hover:bg-red-950/40"
      >
        <LogOut className="h-4 w-4 shrink-0" />
        <span className={labelClass(collapsed)}>{t('nav.logout')}</span>
      </button>
    </div>
  );
}
