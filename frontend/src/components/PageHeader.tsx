import type { ReactNode } from 'react';
import BackButton from './BackButton';

export default function PageHeader({
  title,
  subtitle,
  backTo,
  action,
}: {
  title: string;
  subtitle?: string;
  backTo?: string;
  action?: ReactNode;
}) {
  return (
    <div className="mb-5 flex items-center gap-3">
      {backTo !== undefined && <BackButton to={backTo} />}
      <div className="flex-1">
        <h2 className="text-xl font-bold text-slate-800 dark:text-slate-100">{title}</h2>
        {subtitle && <p className="text-sm text-slate-500 dark:text-slate-400">{subtitle}</p>}
      </div>
      {action}
    </div>
  );
}
