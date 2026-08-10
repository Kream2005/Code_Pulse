import type { ReactNode } from 'react';

export default function Card({
  children,
  className = '',
  padded = false,
}: {
  children: ReactNode;
  className?: string;
  padded?: boolean;
}) {
  return (
    <div
      className={`rounded-lg border border-slate-200 bg-white shadow-sm dark:border-slate-700 dark:bg-slate-900 ${
        padded ? 'p-5' : 'overflow-hidden'
      } ${className}`}
    >
      {children}
    </div>
  );
}
