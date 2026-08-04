import { useI18n } from '../i18n/I18nContext';

type Props = {
  page: number;
  totalPages: number;
  totalElements: number;
  size: number;
  onPageChange: (page: number) => void;
  onSizeChange?: (size: number) => void;
};

export default function Pagination({
  page,
  totalPages,
  totalElements,
  size,
  onPageChange,
  onSizeChange,
}: Props) {
  const { t } = useI18n();
  const safeTotal = Math.max(totalPages, 1);
  return (
    <div className="flex flex-wrap items-center justify-between gap-3 border-t border-slate-200 bg-white px-5 py-3 text-sm text-slate-600 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-300">
      <p>
        {totalElements} {t('common.items')} · {t('common.page')} {page + 1} / {safeTotal}
      </p>
      <div className="flex items-center gap-2">
        {onSizeChange && (
          <select
            value={size}
            onChange={(e) => onSizeChange(Number(e.target.value))}
            className="rounded-lg border border-slate-300 bg-white px-2 py-1.5 text-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
          >
            {[10, 20, 50].map((n) => (
              <option key={n} value={n}>
                {n} {t('common.perPage')}
              </option>
            ))}
          </select>
        )}
        <button
          type="button"
          disabled={page <= 0}
          onClick={() => onPageChange(page - 1)}
          className="rounded-lg border border-slate-300 px-3 py-1.5 font-medium transition hover:border-brand hover:text-brand disabled:opacity-40 dark:border-slate-600"
        >
          {t('common.previous')}
        </button>
        <button
          type="button"
          disabled={page + 1 >= safeTotal}
          onClick={() => onPageChange(page + 1)}
          className="rounded-lg border border-slate-300 px-3 py-1.5 font-medium transition hover:border-brand hover:text-brand disabled:opacity-40 dark:border-slate-600"
        >
          {t('common.next')}
        </button>
      </div>
    </div>
  );
}
