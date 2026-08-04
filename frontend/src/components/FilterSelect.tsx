type Option = { value: string; label: string };

type Props = {
  value: string;
  onChange: (value: string) => void;
  options: Option[];
  allLabel: string;
  className?: string;
};

/** Generic dropdown filter (status/type/role/…) that triggers an immediate API call on change. */
export default function FilterSelect({ value, onChange, options, allLabel, className }: Props) {
  return (
    <select
      value={value}
      onChange={(e) => onChange(e.target.value)}
      className={`rounded-lg border border-slate-300 bg-white px-3 py-1.5 text-sm text-slate-700 focus:border-brand focus:outline-none focus:ring-1 focus:ring-brand dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 ${className ?? ''}`}
    >
      <option value="">{allLabel}</option>
      {options.map((o) => (
        <option key={o.value} value={o.value}>
          {o.label}
        </option>
      ))}
    </select>
  );
}
