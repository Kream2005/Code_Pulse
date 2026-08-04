import type { ReactNode } from 'react';
import Sidebar from './Sidebar';
import Header from './Header';

export default function Layout({ children }: { children: ReactNode }) {
  return (
    <div className="flex h-screen overflow-hidden bg-slate-50 dark:bg-slate-950">
      <Sidebar />
      <div className="relative flex flex-1 flex-col overflow-hidden">
        <img
          src="/flower-logo.png"
          alt=""
          className="pointer-events-none absolute -bottom-10 -right-10 w-200 select-none opacity-[0.5] dark:opacity-[0.15] dark:brightness-0 dark:invert"
        />
        <Header />
        <main className="relative flex-1 overflow-y-auto p-8 text-slate-800 dark:text-slate-100">
          {children}
        </main>
      </div>
    </div>
  );
}
