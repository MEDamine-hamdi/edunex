import { ReactNode } from "react";
import { LanguageSwitcher } from "./LanguageSwitcher";
import { Logo } from "./Logo";
import { ThemeSwitcher } from "./ThemeSwitcher";

export function AuthLayout({ title, subtitle, children }: { title: string; subtitle?: string; children: ReactNode }) {
  return (
    <main className="relative flex min-h-screen items-center justify-center bg-stone-50 px-6 py-24">
      <header className="absolute inset-x-0 top-0 z-10 flex items-center justify-between px-6 py-5">
        <Logo />
        <div className="flex items-center gap-3">
          <ThemeSwitcher />
          <LanguageSwitcher />
        </div>
      </header>
      <div className="w-full max-w-md rounded-3xl bg-white p-8 shadow-xl shadow-stone-900/5 ring-1 ring-stone-100 sm:p-10">
        <h1 className="text-2xl font-extrabold tracking-tight text-stone-900">{title}</h1>
        {subtitle && <p className="mt-2 text-stone-600">{subtitle}</p>}
        <div className="mt-6">{children}</div>
      </div>
    </main>
  );
}

export const authInput =
  "mt-2 w-full rounded-2xl border border-stone-200 bg-stone-50/80 px-4 py-3.5 text-base text-stone-900 placeholder:text-stone-400 focus:border-indigo-400 focus:bg-white focus:outline-none focus:ring-4 focus:ring-indigo-100";
export const authButton =
  "mt-6 flex w-full items-center justify-center rounded-2xl bg-indigo-600 px-4 py-3.5 text-base font-semibold text-white shadow-lg shadow-indigo-600/25 hover:bg-indigo-700 focus:outline-none focus:ring-4 focus:ring-indigo-200 disabled:cursor-not-allowed disabled:opacity-60";