import LocaleThemeControls from './LocaleThemeControls';

export default function Header() {
  return (
    <header className="flex shrink-0 items-center justify-end gap-3 border-b border-slate-200 bg-white px-6 py-2.5 dark:border-slate-700 dark:bg-slate-900">
      <LocaleThemeControls />
    </header>
  );
}
