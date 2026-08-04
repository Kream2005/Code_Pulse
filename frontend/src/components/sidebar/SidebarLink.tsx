import type { ReactNode } from 'react';
import { NavLink } from 'react-router-dom';
import { labelClass } from './labelClass';

export default function SidebarLink({
  to,
  end,
  label,
  icon,
  collapsed,
}: {
  to: string;
  end: boolean;
  label: string;
  icon: ReactNode;
  collapsed: boolean;
}) {
  return (
    <NavLink
      to={to}
      end={end}
      title={collapsed ? label : undefined}
      className={({ isActive }) =>
        `mx-1 my-1 flex items-center rounded-lg py-3 text-sm font-medium transition-colors ${
          collapsed ? 'justify-center px-0' : 'gap-3 px-[22px]'
        } ${
          isActive
            ? 'bg-brand text-white'
            : 'text-slate-600 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-800'
        }`
      }
    >
      {icon}
      <span className={`flex-1 text-left ${labelClass(collapsed)}`}>{label}</span>
    </NavLink>
  );
}
