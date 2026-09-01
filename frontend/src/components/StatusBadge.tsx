import { useI18n } from '../i18n/I18nContext';

export default function StatusBadge({ status }: { status: string }) {
  const { t } = useI18n();
  const map: Record<string, string> = {
    SUBMITTED: 'bg-emerald-50 text-emerald-700 ring-emerald-600/20 dark:bg-emerald-950/40 dark:text-emerald-300',
    SOUMIS: 'bg-emerald-50 text-emerald-700 ring-emerald-600/20 dark:bg-emerald-950/40 dark:text-emerald-300',
    NOT_SUBMITTED: 'bg-amber-50 text-amber-700 ring-amber-600/20 dark:bg-amber-950/40 dark:text-amber-300',
    IN_PROGRESS: 'bg-sky-50 text-sky-700 ring-sky-600/20 dark:bg-sky-950/40 dark:text-sky-300',
    EN_ATTENTE: 'bg-amber-50 text-amber-700 ring-amber-600/20 dark:bg-amber-950/40 dark:text-amber-300',
    EN_COURS: 'bg-sky-50 text-sky-700 ring-sky-600/20 dark:bg-sky-950/40 dark:text-sky-300',
    NON_SOUMIS: 'bg-amber-50 text-amber-700 ring-amber-600/20 dark:bg-amber-950/40 dark:text-amber-300',
    ENVOYEE: 'bg-sky-50 text-sky-700 ring-sky-600/20 dark:bg-sky-950/40 dark:text-sky-300',
    LUE: 'bg-emerald-50 text-emerald-700 ring-emerald-600/20 dark:bg-emerald-950/40 dark:text-emerald-300',
    ECHEC: 'bg-red-50 text-red-700 ring-red-600/20 dark:bg-red-950/40 dark:text-red-300',
    LIEN_ENVOYE: 'bg-sky-50 text-sky-700 ring-sky-600/20 dark:bg-sky-950/40 dark:text-sky-300',
    MOT_DE_PASSE_TEMPORAIRE:
      'bg-violet-50 text-violet-700 ring-violet-600/20 dark:bg-violet-950/40 dark:text-violet-300',
    REJETEE: 'bg-red-50 text-red-700 ring-red-600/20 dark:bg-red-950/40 dark:text-red-300',
  };
  const c = map[status] ?? 'bg-slate-100 text-slate-600 ring-slate-500/20 dark:bg-slate-800 dark:text-slate-300';
  const label = t(`status.${status}`) !== `status.${status}` ? t(`status.${status}`) : status;
  return (
    <span className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-medium ring-1 ring-inset ${c}`}>
      {label}
    </span>
  );
}
