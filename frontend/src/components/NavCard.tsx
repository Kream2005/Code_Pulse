import { Link } from 'react-router-dom';

export default function NavCard({
  to,
  title,
  subtitle,
}: {
  to: string;
  title: string;
  subtitle: string;
}) {
  return (
    <Link
      to={to}
      className="group flex items-center justify-between rounded-lg border border-slate-200 bg-white p-4 shadow-sm transition hover:border-brand hover:shadow dark:border-slate-700 dark:bg-slate-900"
    >
      <div>
        <p className="font-semibold text-slate-800 dark:text-slate-100">{title}</p>
        <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">{subtitle}</p>
      </div>
      <svg
        viewBox="0 0 20 20"
        fill="currentColor"
        className="h-5 w-5 text-slate-300 transition group-hover:translate-x-0.5 group-hover:text-brand dark:text-slate-600"
      >
        <path
          fillRule="evenodd"
          d="M7 4l6 6-6 6-1.4-1.4L10.2 10 5.6 5.4 7 4z"
          clipRule="evenodd"
        />
      </svg>
    </Link>
  );
}
