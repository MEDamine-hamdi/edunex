"use client";

import { createContext, ReactNode, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { setLocaleCode } from "@/lib/format";
import { DICT } from "./dictionary";

export type Lang = "fr" | "en" | "ar";
export const LANGS: { code: Lang; label: string; short: string; locale: string }[] = [
  { code: "fr", label: "Français", short: "FR", locale: "fr-FR" },
  { code: "en", label: "English", short: "EN", locale: "en-GB" },
  { code: "ar", label: "العربية", short: "AR", locale: "ar-TN" },
];

type Ctx = { lang: Lang; setLang: (l: Lang) => void; t: (text: string) => string };
const LanguageContext = createContext<Ctx>({ lang: "fr", setLang: () => {}, t: (s) => s });
export const useLanguage = () => useContext(LanguageContext);

const STORAGE_KEY = "edunex_lang";
const ATTRS = ["placeholder", "title", "aria-label"];
const SKIP = new Set(["SCRIPT", "STYLE", "TEXTAREA", "CODE", "PRE", "NOSCRIPT"]);

function translate(text: string, lang: Lang): string {
  if (lang === "fr") return text;
  const trimmed = text.trim();
  if (!trimmed) return text;
  const entry = DICT[trimmed];
  return entry ? text.replace(trimmed, () => entry[lang]) : text;
}

export function LanguageProvider({ children }: { children: ReactNode }) {
  const [lang, setLangState] = useState<Lang>("fr");

  useEffect(() => {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved === "en" || saved === "ar") setLangState(saved);
  }, []);

  const setLang = useCallback((l: Lang) => {
    setLangState(l);
    localStorage.setItem(STORAGE_KEY, l);
  }, []);

  // direction, language attribute and date locale
  useEffect(() => {
    document.documentElement.lang = lang;
    document.documentElement.dir = lang === "ar" ? "rtl" : "ltr";
    setLocaleCode(LANGS.find((l) => l.code === lang)?.locale ?? "fr-FR");
  }, [lang]);

  // live translation of every text node and attribute (only text nodes are edited, React keeps working)
  useEffect(() => {
    const textRecords = new WeakMap<Text, { src: string; out: string }>();
    const attrRecords = new WeakMap<Element, Record<string, { src: string; out: string }>>();

    const doText = (node: Text) => {
      const parent = node.parentElement;
      if (!parent || SKIP.has(parent.tagName)) return;
      const current = node.nodeValue ?? "";
      const record = textRecords.get(node);
      const src = record && record.out === current ? record.src : current;
      const out = translate(src, lang);
      textRecords.set(node, { src, out });
      if (out !== current) node.nodeValue = out;
    };

    const doElement = (el: Element) => {
      for (const attr of ATTRS) {
        if (!el.hasAttribute(attr)) continue;
        const current = el.getAttribute(attr) ?? "";
        const records = attrRecords.get(el) ?? {};
        const record = records[attr];
        const src = record && record.out === current ? record.src : current;
        const out = translate(src, lang);
        records[attr] = { src, out };
        attrRecords.set(el, records);
        if (out !== current) el.setAttribute(attr, out);
      }
    };

    const walk = (root: Node) => {
      if (root.nodeType === Node.TEXT_NODE) {
        doText(root as Text);
        return;
      }
      if (root.nodeType !== Node.ELEMENT_NODE) return;
      const el = root as Element;
      if (SKIP.has(el.tagName)) return;
      doElement(el);
      const walker = document.createTreeWalker(el, NodeFilter.SHOW_TEXT | NodeFilter.SHOW_ELEMENT);
      let node: Node | null = walker.nextNode();
      while (node) {
        if (node.nodeType === Node.TEXT_NODE) doText(node as Text);
        else doElement(node as Element);
        node = walker.nextNode();
      }
    };

    walk(document.body);
    const observer = new MutationObserver((mutations) => {
      for (const m of mutations) {
        if (m.type === "characterData") doText(m.target as Text);
        else if (m.type === "attributes") doElement(m.target as Element);
        else m.addedNodes.forEach((n) => walk(n));
      }
    });
    observer.observe(document.body, {
      childList: true,
      subtree: true,
      characterData: true,
      attributes: true,
      attributeFilter: ATTRS,
    });
    return () => observer.disconnect();
  }, [lang]);

  const value = useMemo<Ctx>(() => ({ lang, setLang, t: (text) => translate(text, lang) }), [lang, setLang]);
  return <LanguageContext.Provider value={value}>{children}</LanguageContext.Provider>;
}