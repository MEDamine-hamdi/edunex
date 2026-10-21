"use client";

import { useEffect, useRef, useState } from "react";
import { LANGS, useLanguage } from "@/i18n/LanguageProvider";
import { ChevronDownIcon, GlobeIcon } from "./icons";

export function LanguageSwitcher({ className = "" }: { className?: string }) {
  const { lang, setLang } = useLanguage();
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const close = (e: MouseEvent) => {
      if (!ref.current?.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener("mousedown", close);
    return () => document.removeEventListener("mousedown", close);
  }, []);

  const current = LANGS.find((l) => l.code === lang) ?? LANGS[0];
  return (
    <div ref={ref} className={`relative ${className}`}>
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        aria-haspopup="listbox"
        aria-expanded={open}
        className="flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm font-semibold text-slate-800 shadow-sm hover:bg-slate-50"
      >
        <GlobeIcon className="h-5 w-5" /> {current.short} <ChevronDownIcon className="h-4 w-4" />
      </button>
      {open && (
        <ul role="listbox" className="absolute end-0 z-50 mt-2 w-40 overflow-hidden rounded-xl border border-slate-200 bg-white py-1 shadow-lg">
          {LANGS.map((l) => (
            <li key={l.code}>
              <button
                type="button"
                onClick={() => {
                  setLang(l.code);
                  setOpen(false);
                }}
                className={`block w-full px-4 py-2 text-start text-sm hover:bg-slate-50 ${l.code === lang ? "font-semibold text-blue-700" : "text-slate-700"}`}
              >
                {l.label}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}