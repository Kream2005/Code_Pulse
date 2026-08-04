export default function StatCard({
  label,
  value,
  accent = 'text-slate-800 dark:text-slate-100',
}: {
  label: string;
  value: number | string;
  accent?: string;
}) {
  return (
    <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm dark:border-slate-700 dark:bg-slate-900">
      <p className="text-sm font-medium text-slate-500 dark:text-slate-400">{label}</p>
      <p className={`mt-1 text-3xl font-bold ${accent}`}>{value}</p>
    </div>
  );
}
