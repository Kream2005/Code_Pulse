import { useEffect, useRef, useState } from 'react';
import { useI18n } from '../i18n/I18nContext';

type Props = {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  debounceMs?: number;
  className?: string;
  autoFocus?: boolean;
};

export default function SearchInput({
  value,
  onChange,
  placeholder,
  debounceMs = 300,
  className,
  autoFocus,
}: Props) {
  const { t } = useI18n();
  const [draft, setDraft] = useState(value);
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    setDraft(value);
  }, [value]);

  useEffect(() => {
    return () => {
      if (timerRef.current) clearTimeout(timerRef.current);
    };
  }, []);

  function handleChange(next: string) {
    setDraft(next);
    if (timerRef.current) clearTimeout(timerRef.current);
    timerRef.current = setTimeout(() => onChange(next), debounceMs);
  }

  function handleClear() {
    if (timerRef.current) clearTimeout(timerRef.current);
    setDraft('');
    onChange('');
  }

  return (
    <div className={`relative ${className ?? ''}`}>
      <svg
        className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400"
        fill="none"
        viewBox="0 0 24 24"
        stroke="currentColor"
      >
        <path
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth={2}
          d="M21 21l-4.35-4.35m0 0A7.5 7.5 0 1 0 5.4 5.4a7.5 7.5 0 0 0 11.25 11.25z"
        />
      </svg>
      <input
        type="text"
        value={draft}
        autoFocus={autoFocus}
        onChange={(e) => handleChange(e.target.value)}
        placeholder={placeholder ?? t('common.searchPlaceholder')}
        className="w-full rounded-lg border border-slate-300 bg-white py-1.5 pl-9 pr-8 text-sm text-slate-700 placeholder:text-slate-400 focus:border-brand focus:outline-none focus:ring-1 focus:ring-brand dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
      />
      {draft && (
        <button
          type="button"
          onClick={handleClear}
          aria-label={t('common.clearFilters')}
          className="absolute right-2 top-1/2 -translate-y-1/2 rounded-full p-0.5 text-slate-400 transition hover:bg-slate-100 hover:text-slate-600 dark:hover:bg-slate-700"
        >
          <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      )}
    </div>
  );
}
