import { useState } from 'react';
import type { LucideIcon } from 'lucide-react';
import { Eye, EyeOff } from 'lucide-react';

export default function FormField({
  label,
  icon: Icon,
  type = 'text',
  value,
  onChange,
  placeholder,
  minLength,
}: {
  label: string;
  icon: LucideIcon;
  type?: string;
  value: string;
  onChange: (v: string) => void;
  placeholder?: string;
  minLength?: number;
}) {
  const [reveal, setReveal] = useState(false);
  const isPassword = type === 'password';

  return (
    <label className="mb-5 block">
      <span className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-200">
        {label}
      </span>
      <div className="relative">
        <span className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3.5 text-slate-400">
          <Icon className="h-5 w-5" />
        </span>
        <input
          type={isPassword && reveal ? 'text' : type}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          required
          minLength={minLength}
          placeholder={placeholder}
          className={`w-full rounded-lg border border-slate-300 bg-white py-2.5 pl-11 text-slate-800 outline-none transition placeholder:text-slate-400 focus:border-brand focus:ring-2 focus:ring-brand/20 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 ${
            isPassword ? 'pr-11' : 'pr-3.5'
          }`}
        />
        {isPassword && (
          <button
            type="button"
            onClick={() => setReveal((v) => !v)}
            className="absolute inset-y-0 right-0 flex items-center pr-3.5 text-slate-400 transition hover:text-slate-600 dark:hover:text-slate-200"
          >
            {reveal ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
          </button>
        )}
      </div>
    </label>
  );
}
