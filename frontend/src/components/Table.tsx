import type { ReactNode } from 'react';
import { useI18n } from '../i18n/I18nContext';

export default function Table({
  columns,
  children,
  isEmpty,
  emptyLabel,
}: {
  columns: string[];
  children: ReactNode;
  isEmpty?: boolean;
  emptyLabel?: string;
}) {
  const { t } = useI18n();
  return (
    <table className="w-full text-left text-sm">
      <thead className="border-b border-slate-200 bg-slate-50 text-xs uppercase tracking-wide text-slate-500 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-400">
        <tr>
          {columns.map((c) => (
            <th key={c} className="px-5 py-3 font-medium">
              {c}
            </th>
          ))}
        </tr>
      </thead>
      <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
        {isEmpty ? (
          <tr>
            <td
              colSpan={columns.length}
              className="px-5 py-8 text-center text-slate-400 dark:text-slate-500"
            >
              {emptyLabel ?? t('common.empty')}
            </td>
          </tr>
        ) : (
          children
        )}
      </tbody>
    </table>
  );
}
