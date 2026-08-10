import { Moon, Sun } from 'lucide-react';
import { useI18n } from '../i18n/I18nContext';
import type { Lang } from '../i18n/translations';

const LANGS: Lang[] = ['FR', 'EN'];

export default function LocaleThemeControls({ className = '' }: { className?: string }) {
  const { lang, setLang, theme, toggleTheme, t } = useI18n();
  const dark = theme === 'dark';

  return (
    <div className={`flex items-center justify-end gap-3 ${className}`}>
      <div className="flex overflow-hidden rounded-lg border border-slate-300 dark:border-slate-600">
        {LANGS.map((l) => (
          <button
            key={l}
            type="button"
            onClick={() => setLang(l)}
            aria-pressed={lang === l}
            className={`rounded-none px-3 py-1.5 text-xs font-semibold transition ${
              lang === l
                ? 'bg-brand text-white'
                : 'bg-white text-slate-600 hover:bg-slate-50 dark:bg-slate-800 dark:text-slate-200 dark:hover:bg-slate-700'
            }`}
          >
            {l}
          </button>
        ))}
      </div>

      <button
        type="button"
        onClick={toggleTheme}
        title={dark ? t('header.light') : t('header.dark')}
        aria-label={dark ? t('header.light') : t('header.dark')}
        className="flex h-9 w-9 items-center justify-center rounded-lg border border-slate-300 text-slate-600 transition hover:border-brand hover:text-brand dark:border-slate-600 dark:text-slate-200 dark:hover:border-brand"
      >
        {dark ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
      </button>
    </div>
  );
}
